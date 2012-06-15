package cz.cesnet.shongo.controller;

import org.junit.After;
import org.junit.Before;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract database test provides the entity manager to extending classes as protected member variable.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractDatabaseTest
{
    /**
     * Single instance of entity manager factory.
     */
    private static EntityManagerFactory entityManagerFactory;

    /**
     * Entity manager factory for current database test.
     */
    protected EntityManager entityManager;

    /**
     * Initialize test.
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception
    {
        if ( entityManagerFactory == null ) {
            // For testing purposes use only in-memory database
            Map<String, String> properties = new HashMap<String, String>();
            properties.put("hibernate.connection.url", "jdbc:hsqldb:mem:controller; shutdown=true;");
            entityManagerFactory = Persistence.createEntityManagerFactory("controller", properties);
        }
        entityManager = entityManagerFactory.createEntityManager();
    }

    /**
     * Clean-up test
     */
    @After
    public void tearDown()
    {
        if ( entityManager != null ) {
            entityManager.close();
        }
    }
}
