package cz.cesnet.shongo.controller;

import org.junit.After;
import org.junit.Before;

import javax.persistence.*;

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
            entityManagerFactory = Persistence.createEntityManagerFactory("test");
        }
        entityManager = entityManagerFactory.createEntityManager();
    }

    /**
     * Clean-up test
     */
    @After
    public void tearDown()
    {
        entityManager.close();
    }
}
