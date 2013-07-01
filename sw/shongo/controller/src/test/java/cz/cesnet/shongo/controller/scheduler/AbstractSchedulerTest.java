package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.*;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;

import javax.persistence.EntityManager;

/**
 * Tests for {@link cz.cesnet.shongo.controller.scheduler.CompartmentReservationTask}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractSchedulerTest extends AbstractDatabaseTest
{
    private Cache cache;
    private EntityManager entityManager;

    @Before
    public void before() throws Exception
    {
        super.before();

        Domain.setLocalDomain(new Domain("test"));

        // Init cache
        cache =  new Cache();
        cache.init();

        // Create entity manager
        entityManager = super.createEntityManager();
    }

    @After
    public void after() throws Exception
    {
        super.after();

        if (entityManager != null) {
            entityManager.close();
        }

        cache.destroy();

        Domain.setLocalDomain(null);
    }

    public Cache getCache()
    {
        return cache;
    }

    protected EntityManager getEntityManager()
    {
        return entityManager;
    }

    public SchedulerContext createSchedulerContext(Interval interval)
    {
        return new SchedulerContext(cache, entityManager, interval);
    }

    public SchedulerContext createSchedulerContext()
    {
        return new SchedulerContext(cache, entityManager, Temporal.INTERVAL_INFINITE);
    }

    protected void createResource(Resource resource)
    {
        resource.setUserId(Authorization.ROOT_USER_ID);
        resource.setName("resource");

        entityManager.getTransaction().begin();
        cache.addResource(resource, entityManager);
        entityManager.getTransaction().commit();
    }

    protected void createReservation(Reservation reservation)
    {
        entityManager.getTransaction().begin();
        entityManager.persist(reservation);
        entityManager.getTransaction().commit();
    }
}
