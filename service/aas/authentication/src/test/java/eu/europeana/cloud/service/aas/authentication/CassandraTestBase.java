package eu.europeana.cloud.service.aas.authentication;

import java.io.IOException;

import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

/**
 * Test configuration for Cassandra
 */
public abstract class CassandraTestBase {

    private static final Logger LOGGER = LoggerFactory
	    .getLogger(CassandraTestBase.class);

    private static boolean serverRunning = false;

    private static Cluster cluster;

    /**
     * CassandraTestBase KEYSPACE_SCHEMA_CQL
     */
    // config:
    public static final String KEYSPACE_SCHEMA_CQL = "cassandra-aas.cql";

    /**
     * CassandraTestBase PORT
     */
    public static final int PORT = 29142;

    /**
     * CassandraTestBase KEYSPACE
     */
    public static final String KEYSPACE = "aas_test";

    /**
     * CassandraTestBase CASSANDRA_CONFIG_FILE
     */
    public static final String CASSANDRA_CONFIG_FILE = "cassandra.yaml";

    /**
     * Creates a new instance of this class.
     */
    public CassandraTestBase() {
	synchronized (CassandraTestBase.class) {
	    if (!serverRunning) {
		try {
		    LOGGER.info("Starting embedded Cassandra...");
		    EmbeddedCassandraServerHelper
			    .startEmbeddedCassandra(CASSANDRA_CONFIG_FILE);
		    try {
			Thread.sleep(10000l);
		    } catch (InterruptedException e) {
			throw new RuntimeException(
				"Caused by InterruptedException", e);
		    }
		} catch (Exception e) {
		    LOGGER.error("Cannot start embedded Cassandra!", e);
		    throw new RuntimeException(
			    "Cannot start embedded Cassandra!", e);
		}
		cluster = Cluster.builder().addContactPoint("localhost")
			.withPort(PORT).build();
		createKeyspaces();
		serverRunning = true;
	    }
	}
    }

    /**
     * @return The session to connect
     */
    protected Session getSession() {
	return cluster.connect(KEYSPACE);
    }

    public void createKeyspaces() {
	try {
	    LOGGER.info("Initializing keyspaces...");
	    initKeyspace();
	} catch (IOException e) {
	    LOGGER.error("Cannot Initialize keyspaces!", e);
	    EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
	    throw new RuntimeException("Cannot Initialize keyspaces!", e);
	}
    }

    public void dropAllKeyspaces() {
	LOGGER.info("Drop all keyspaces...");
	EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }

    private void initKeyspace() throws IOException {
	CQLDataLoader dataLoader = new CQLDataLoader(cluster.newSession());
	dataLoader.load(new ClassPathCQLDataSet(KEYSPACE_SCHEMA_CQL, KEYSPACE));
    }
}
