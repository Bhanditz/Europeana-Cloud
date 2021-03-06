package eu.europeana.cloud.service.dps.utils.files.counter;

import eu.europeana.cloud.common.model.dps.TaskInfo;
import eu.europeana.cloud.common.model.dps.TaskState;
import eu.europeana.cloud.service.dps.DpsTask;
import eu.europeana.cloud.service.dps.PluginParameterKeys;
import eu.europeana.cloud.service.dps.exception.TaskInfoDoesNotExistException;
import eu.europeana.cloud.service.dps.rest.exceptions.TaskSubmissionException;
import eu.europeana.cloud.service.dps.storm.utils.CassandraTaskInfoDAO;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Tarek on 5/25/2018.
 */
public class DatasetFilesCounterTest {

    private DatasetFilesCounter datasetFilesCounter;
    private CassandraTaskInfoDAO cassandraTaskInfoDAO;
    private static final long TASK_ID = 1234;
    private static final int EXPECTED_SIZE = 100;
    private static final int DEFAULT_FILES_COUNT = -1;
    private static final String TOPOLOGY_NAME = "TOPOLOGY_NAME";
    private DpsTask dpsTask;


    @Before
    public void init() {
        cassandraTaskInfoDAO = mock(CassandraTaskInfoDAO.class);
        datasetFilesCounter = new DatasetFilesCounter(cassandraTaskInfoDAO);
        dpsTask = new DpsTask();
    }


    @Test
    public void shouldReturnProcessedFiles() throws Exception {
        TaskInfo taskInfo = new TaskInfo(TASK_ID, TOPOLOGY_NAME, TaskState.PROCESSED, "", EXPECTED_SIZE, EXPECTED_SIZE, 0, new Date(), new Date(), new Date());
        when(cassandraTaskInfoDAO.searchById(TASK_ID)).thenReturn(taskInfo);
        dpsTask.addParameter(PluginParameterKeys.PREVIOUS_TASK_ID, String.valueOf(TASK_ID));
        int expectedFilesCount = datasetFilesCounter.getFilesCount(dpsTask);
        assertEquals(EXPECTED_SIZE, expectedFilesCount);
    }

    @Test
    public void shouldReturnedDefaultFilesCountWhenNoPreviousTaskIdIsProvided() throws Exception {
        int expectedFilesCount = datasetFilesCounter.getFilesCount(dpsTask);
        assertEquals(DEFAULT_FILES_COUNT, expectedFilesCount);
    }

    @Test
    public void shouldReturnedDefaultFilesCountWhenPreviousTaskIdIsNotLongValue() throws Exception {
        int expectedFilesCount = datasetFilesCounter.getFilesCount(dpsTask);
        dpsTask.addParameter(PluginParameterKeys.PREVIOUS_TASK_ID, "Not long value");
        assertEquals(DEFAULT_FILES_COUNT, expectedFilesCount);
    }

    @Test
    public void shouldReturnedDefaultFilesCountWhenPreviousTaskIdDoesNotExist() throws Exception {
        dpsTask.addParameter(PluginParameterKeys.PREVIOUS_TASK_ID, String.valueOf(TASK_ID));
        doThrow(TaskInfoDoesNotExistException.class).when(cassandraTaskInfoDAO).searchById(TASK_ID);
        int expectedFilesCount = datasetFilesCounter.getFilesCount(dpsTask);
        assertEquals(DEFAULT_FILES_COUNT, expectedFilesCount);
    }

    @Test(expected = TaskSubmissionException.class)
    public void shouldThrowExceptionWhenQueryingDatabaseUsingPreviousTaskIdThrowAnExceptionOtherThanTaskInfoDoesNotExistException() throws Exception {
        dpsTask.addParameter(PluginParameterKeys.PREVIOUS_TASK_ID, String.valueOf(TASK_ID));
        doThrow(Exception.class).when(cassandraTaskInfoDAO).searchById(TASK_ID);
        datasetFilesCounter.getFilesCount(dpsTask);
    }


}