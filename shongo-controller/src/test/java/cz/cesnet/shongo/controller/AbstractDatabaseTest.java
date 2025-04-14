package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.util.Timer;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract database test provides the entity manager to extending classes as protected member variable.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractDatabaseTest
{
    private static Logger logger = LoggerFactory.getLogger(AbstractDatabaseTest.class);

    /**
     * Connection.
     */
    protected static String connectionDriver = "org.hsqldb.jdbcDriver";
    protected static String connectionUrl = "jdbc:hsqldb:mem:test; shutdown=true; sql.syntax_pgs=true;";

    /**
     * Enable driver for debugging SQL.
     */
    protected static synchronized void enableDebugDriver()
    {
        connectionDriver = "net.sf.log4jdbc.DriverSpy";
        connectionUrl = connectionUrl.replace("jdbc:", "jdbc:log4jdbc:");
    }

    /**
     * Single instance of entity manager factory.
     */
    private static EntityManagerFactory entityManagerFactory;

    /**
     * @return entity manager factory
     */
    protected EntityManagerFactory getEntityManagerFactory()
    {
        synchronized (AbstractControllerTest.class) {
            return entityManagerFactory;
        }
    }

    /**
     * @return entity manager
     */
    protected EntityManager createEntityManager()
    {
        synchronized (AbstractControllerTest.class) {
            return entityManagerFactory.createEntityManager();
        }
    }

    /**
     * Perform tests initialization.
     *
     * @throws Exception
     */
    @Before
    public void before() throws Exception
    {
        synchronized (AbstractControllerTest.class) {
            if (entityManagerFactory == null) {
                // For testing purposes use only in-memory database
                Map<String, String> properties = new HashMap<String, String>();
                properties.put("hibernate.connection.driver_class", connectionDriver);
                properties.put("hibernate.connection.url", connectionUrl);
                properties.put("hibernate.connection.username", "sa");
                properties.put("hibernate.connection.password", "");

                logger.info("Creating entity manager factory...");
                Timer timer = new Timer();
                entityManagerFactory = Persistence.createEntityManagerFactory("controller", properties);
                logger.info("Entity manager factory created in {} ms.", timer.stop());

                Controller.initializeDatabase(entityManagerFactory);
            }
            else {
                logger.info("Reusing existing entity manager factory.");
                clearData();
            }
        }
    }

    /**
     * Perform tests clean-up.
     */
    @After
    public void after() throws Exception
    {
        // Do not close entity manager factory to allow re-usage of it for the next test
    }

    /**
     * Clear data in {@link #entityManagerFactory}.
     */
    protected void clearData()
    {
        synchronized (AbstractControllerTest.class) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            try {
                logger.info("Clearing database data...");
                entityManager.getTransaction().begin();
                entityManager.createNativeQuery(
                        "TRUNCATE SCHEMA PUBLIC RESTART IDENTITY AND COMMIT NO CHECK").executeUpdate();
                entityManager.getTransaction().commit();
                logger.info("Database data cleared.");
            }
            finally {
                entityManager.close();
            }
        }
    }
}
