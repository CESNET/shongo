package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.*;
import cz.cesnet.shongo.controller.scheduler.SchedulerContext;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;

import javax.persistence.EntityManager;

/**
 * Tests for {@link cz.cesnet.shongo.controller.booking.compartment.CompartmentReservationTask}.
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

        LocalDomain.setLocalDomain(new LocalDomain("test"));
        Reporter.create();

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

        Reporter.getInstance().destroy();
        LocalDomain.setLocalDomain(null);
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
        return new SchedulerContext(interval.getStart(), cache, entityManager,
                new AuthorizationManager(entityManager, new DummyAuthorization(getEntityManagerFactory())));
    }

    public SchedulerContext createSchedulerContext()
    {
        SchedulerContext schedulerContext = createSchedulerContext(Temporal.INTERVAL_INFINITE);
        schedulerContext.setUserId("0");
        return schedulerContext;
    }

    protected void createResource(Resource resource)
    {
        resource.setUserId(Authorization.ROOT_USER_ID);
        resource.setName("newResource");

        entityManager.getTransaction().begin();
        cache.addResource(resource, entityManager);
        entityManager.getTransaction().commit();
    }

    protected void createReservation(Reservation reservation)
    {
        reservation.setUserId("0");
        entityManager.getTransaction().begin();
        entityManager.persist(reservation);
        entityManager.getTransaction().commit();
    }
}
