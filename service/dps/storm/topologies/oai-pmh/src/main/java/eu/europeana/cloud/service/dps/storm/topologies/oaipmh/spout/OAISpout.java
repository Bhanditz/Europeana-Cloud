package eu.europeana.cloud.service.dps.storm.topologies.oaipmh.spout;

import com.rits.cloning.Cloner;
import eu.europeana.cloud.common.model.dps.TaskState;
import eu.europeana.cloud.service.dps.DpsTask;
import eu.europeana.cloud.service.dps.InputDataType;
import eu.europeana.cloud.service.dps.OAIPMHHarvestingDetails;
import eu.europeana.cloud.service.dps.PluginParameterKeys;
import eu.europeana.cloud.service.dps.storm.NotificationTuple;
import eu.europeana.cloud.service.dps.storm.StormTaskTuple;
import eu.europeana.cloud.service.dps.storm.spouts.kafka.CustomKafkaSpout;
import eu.europeana.cloud.service.dps.storm.spouts.kafka.utils.TaskSpoutInfo;
import eu.europeana.cloud.service.dps.storm.topologies.oaipmh.helpers.SourceProvider;
import eu.europeana.cloud.service.dps.storm.topologies.oaipmh.spout.schema.SchemaFactory;
import eu.europeana.cloud.service.dps.storm.topologies.oaipmh.spout.schema.SchemaHandler;
import org.apache.storm.kafka.SpoutConfig;
import org.apache.storm.spout.ISpoutOutputCollector;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.codehaus.jackson.map.ObjectMapper;
import org.dspace.xoai.model.oaipmh.Header;
import org.dspace.xoai.serviceprovider.exceptions.BadArgumentException;
import org.dspace.xoai.serviceprovider.parameters.ListIdentifiersParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

import static eu.europeana.cloud.service.dps.storm.AbstractDpsBolt.NOTIFICATION_STREAM_NAME;

/**
 * Created by Tarek on 4/30/2018.
 */
public class OAISpout extends CustomKafkaSpout {

    private SpoutOutputCollector collector;
    private static final Logger LOGGER = LoggerFactory.getLogger(OAISpout.class);
    private SourceProvider sourceProvider;

    private static final int DEFAULT_RETRIES = 10;
    private static final int SLEEP_TIME = 5000;

    private TaskDownloader taskDownloader;

    public OAISpout(SpoutConfig spoutConf, String hosts, int port, String keyspaceName,
                    String userName, String password) {
        super(spoutConf, hosts, port, keyspaceName, userName, password);

    }


    @Override
    public void open(Map conf, TopologyContext context,
                     SpoutOutputCollector collector) {
        this.collector = collector;
        taskDownloader = new TaskDownloader();
        sourceProvider = new SourceProvider();
        super.open(conf, context, new CollectorWrapper(collector));
    }

    @Override
    public void nextTuple() {
        StormTaskTuple stormTaskTuple = null;
        try {
            super.nextTuple();
            stormTaskTuple = taskDownloader.getTupleWithOAIIdentifier();
            if (stormTaskTuple != null) {
                collector.emit(stormTaskTuple.toStormTuple());
            }
        } catch (Exception e) {
            LOGGER.error("StaticDpsTaskSpout error: {}", e.getMessage());
            if (stormTaskTuple != null)
                cassandraTaskInfoDAO.dropTask(stormTaskTuple.getTaskId(), "The task was dropped because " + e.getMessage(), TaskState.DROPPED.toString());
        }
    }


    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(StormTaskTuple.getFields());
        declarer.declareStream(NOTIFICATION_STREAM_NAME, NotificationTuple.getFields());
    }

    private class CollectorWrapper extends SpoutOutputCollector {

        CollectorWrapper(ISpoutOutputCollector delegate) {
            super(delegate);
        }

        @Override
        public List<Integer> emit(String streamId, List<Object> tuple, Object messageId) {
            try {
                DpsTask dpsTask = new ObjectMapper().readValue((String) tuple.get(0), DpsTask.class);
                if (dpsTask != null) {
                    taskDownloader.fillTheQueue(new TaskSpoutInfo(dpsTask));
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
            return Collections.emptyList();
        }
    }


    class TaskDownloader extends Thread {
        private static final int MAX_SIZE = 100;
        private ArrayBlockingQueue<TaskSpoutInfo> taskQueue = new ArrayBlockingQueue<>(MAX_SIZE);
        private ArrayBlockingQueue<StormTaskTuple> oaiIdentifiers = new ArrayBlockingQueue<>(MAX_SIZE);


        public TaskDownloader() {
            start();
        }

        public StormTaskTuple getTupleWithOAIIdentifier() {
            return oaiIdentifiers.poll();
        }

        public void fillTheQueue(TaskSpoutInfo taskSpoutInfo) {
            try {
                taskQueue.put(taskSpoutInfo);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void run() {
            StormTaskTuple stormTaskTuple = null;
            while (true) {
                try {
                    TaskSpoutInfo taskSpoutInfo = taskQueue.take();
                    DpsTask dpsTask = taskSpoutInfo.getDpsTask();
                    OAIPMHHarvestingDetails oaipmhHarvestingDetails = dpsTask.getHarvestingDetails();
                    if (oaipmhHarvestingDetails == null)
                        oaipmhHarvestingDetails = new OAIPMHHarvestingDetails();
                    stormTaskTuple = new StormTaskTuple(
                            dpsTask.getTaskId(),
                            dpsTask.getTaskName(),
                            dpsTask.getDataEntry(InputDataType.REPOSITORY_URLS).get(0), null, dpsTask.getParameters(), dpsTask.getOutputRevision(), oaipmhHarvestingDetails);
                    execute(stormTaskTuple);
                } catch (Exception e) {
                    LOGGER.error("StaticDpsTaskSpout error: {}", e.getMessage());
                    if (stormTaskTuple != null)
                        cassandraTaskInfoDAO.dropTask(stormTaskTuple.getTaskId(), "The task was dropped because " + e.getMessage(), TaskState.DROPPED.toString());
                }
            }
        }

        public void execute(StormTaskTuple stormTaskTuple) throws BadArgumentException, InterruptedException {
            startProgress(stormTaskTuple);
            SchemaHandler schemaHandler = SchemaFactory.getSchemaHandler(stormTaskTuple);
            Set<String> schemas = schemaHandler.getSchemas(stormTaskTuple);
            OAIPMHHarvestingDetails oaipmhHarvestingDetails = stormTaskTuple.getSourceDetails();
            int expectedSize = 0;

            Date fromDate = oaipmhHarvestingDetails.getDateFrom();
            Date untilDate = oaipmhHarvestingDetails.getDateUntil();
            Set<String> sets = oaipmhHarvestingDetails.getSets();

            for (String schema : schemas) {
                if (sets == null || sets.isEmpty()) {
                    expectedSize += harvestIdentifiers(schema, null, fromDate, untilDate, stormTaskTuple);
                } else
                    for (String set : sets) {
                        expectedSize += harvestIdentifiers(schema, set, fromDate, untilDate, stormTaskTuple);
                    }
            }

            updateTaskBasedOnExpectedSize(stormTaskTuple, expectedSize);
        }

        private void updateTaskBasedOnExpectedSize(StormTaskTuple stormTaskTuple, int expectedSize) {
            if (expectedSize > 0)
                cassandraTaskInfoDAO.setUpdateExpectedSize(stormTaskTuple.getTaskId(), expectedSize);
            else
                cassandraTaskInfoDAO.dropTask(stormTaskTuple.getTaskId(), "The task with the submitted parameters is empty", TaskState.DROPPED.toString());
        }

        private void startProgress(StormTaskTuple stormTaskTuple) {
            cassandraTaskInfoDAO.updateTask(stormTaskTuple.getTaskId(), "", String.valueOf(TaskState.CURRENTLY_PROCESSING), new Date());
        }


        private int harvestIdentifiers(String schema, String dataset, Date fromDate, Date untilDate, StormTaskTuple stormTaskTuple) throws InterruptedException
                , BadArgumentException {
            OAIPMHHarvestingDetails sourceDetails = stormTaskTuple.getSourceDetails();
            String url = stormTaskTuple.getFileUrl();
            ListIdentifiersParameters parameters = configureParameters(schema, dataset, fromDate, untilDate);
            return parseHeaders(sourceProvider.provide(url).listIdentifiers(parameters), sourceDetails.getExcludedSets(), stormTaskTuple, schema);
        }

        /**
         * Configure request parameters
         *
         * @return object representing parameters for ListIdentifiers request
         */
        private ListIdentifiersParameters configureParameters(String schema, String dataset, Date fromDate, Date untilDate) {
            ListIdentifiersParameters parameters = ListIdentifiersParameters.request()
                    .withMetadataPrefix(schema);

            if (fromDate != null)
                parameters.withFrom(fromDate);
            if (untilDate != null)
                parameters.withUntil(untilDate);
            if (dataset != null)
                parameters.withSetSpec(dataset);

            return parameters;
        }

        private void fillIdentifiersQueue(StormTaskTuple stormTaskTuple, String identifier, String schema) throws InterruptedException {
            StormTaskTuple tuple = new Cloner().deepClone(stormTaskTuple);
            tuple.addParameter(PluginParameterKeys.CLOUD_LOCAL_IDENTIFIER, identifier);
            tuple.addParameter(PluginParameterKeys.SCHEMA_NAME, schema);
            tuple.addParameter(PluginParameterKeys.DPS_TASK_INPUT_DATA, stormTaskTuple.getFileUrl());
            tuple.setFileUrl(identifier);
            oaiIdentifiers.put(tuple);

        }


        /**
         * Parse headers returned by the OAI-PMH source
         *
         * @param headerIterator iterator of headers returned by the source
         * @param excludedSets   sets to exclude
         * @param stormTaskTuple tuple to be used for emitting identifier
         * @return number of harvested identifiers
         */
        private int parseHeaders(Iterator<Header> headerIterator, Set<String> excludedSets, StormTaskTuple stormTaskTuple, String schema) throws InterruptedException {
            int count = 0;
            if (headerIterator == null) {
                throw new IllegalArgumentException("Header iterator is null");
            }

            while (hasNext(headerIterator) && !taskStatusChecker.hasKillFlag(stormTaskTuple.getTaskId())) {
                Header header = headerIterator.next();
                if (filterHeader(header, excludedSets)) {
                    fillIdentifiersQueue(stormTaskTuple, header.getIdentifier(), schema);
                    count++;
                }
            }
            return count;

        }

        private boolean hasNext(Iterator<Header> headerIterator) {
            int retries = DEFAULT_RETRIES;
            while (true) {
                try {
                    return headerIterator.hasNext();
                } catch (Exception e) {
                    if (retries-- > 0) {
                        LOGGER.warn("Error while getting the next batch: {}", retries);
                        waitForSpecificTime();
                    } else {
                        LOGGER.error("Error while getting the next batch");
                        throw e;
                    }
                }
            }
        }

        protected void waitForSpecificTime() {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e1) {
                Thread.currentThread().interrupt();
                LOGGER.error(e1.getMessage());
            }
        }


        /**
         * Filter header by checking whether it belongs to any of excluded sets.
         *
         * @param header       header to filter
         * @param excludedSets sets to exclude
         */

        private boolean filterHeader(Header header, Set<String> excludedSets) {
            if (excludedSets != null && !excludedSets.isEmpty()) {
                for (String set : excludedSets) {
                    if (header.getSetSpecs().contains(set)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}


