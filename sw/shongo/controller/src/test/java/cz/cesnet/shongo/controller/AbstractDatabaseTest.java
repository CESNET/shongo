package cz.cesnet.shongo.controller;

import org.junit.After;
import org.junit.Before;

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
    /**
     * Single instance of entity manager factory.
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @return entity manager factory
     */
    protected EntityManagerFactory getEntityManagerFactory()
    {
        return entityManagerFactory;
    }

    /**
     * @return entity manager
     */
    protected EntityManager getEntityManager()
    {
        return entityManagerFactory.createEntityManager();
    }

    /**
     * Perform tests initialization.
     *
     * @throws Exception
     */
    @Before
    public void before() throws Exception
    {
        // For testing purposes use only in-memory database
        Map<String, String> properties = new HashMap<String, String>();
        String schema = getClass().getName().replace(".", "_");
        properties.put("hibernate.connection.url", "jdbc:hsqldb:mem:" + schema + "; shutdown=true;");

        entityManagerFactory = Persistence.createEntityManagerFactory("controller", properties);
    }

    /**
     * Perform tests clean-up.
     */
    @After
    public void after()
    {
        entityManagerFactory.close();
    }
}
