package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * A reservation database for domain controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationDatabase
{
    private static Logger logger = LoggerFactory.getLogger(ReservationDatabase.class);

    /**
     * Domain for which the reservation database is used.
     */
    private Domain domain;

    /**
     * Entity manager that is used for accessing database.
     */
    private EntityManager entityManager;

    /**
     * Scheduler.
     */
    private Scheduler scheduler;

    /**
     * @see ReservationRequestManager
     */
    private ReservationRequestManager reservationRequestManager;

    /**
     * Constructor of reservation database.
     */
    public ReservationDatabase()
    {
    }

    /**
     * Constructor of reservation database.
     *
     * @param entityManager sets the {@link #entityManager}
     * @param domain        sets the {@link #domain}
     * @param scheduler     sets the {@link #scheduler}
     */
    public ReservationDatabase(EntityManager entityManager, Domain domain, Scheduler scheduler)
    {
        setEntityManager(entityManager);
        setDomain(domain);
        setScheduler(scheduler);
        init();
    }

    /**
     * @param domain sets the {@link #domain}
     */
    public void setDomain(Domain domain)
    {
        this.domain = domain;
    }

    /**
     * @param entityManager sets the {@link #entityManager}
     */
    public void setEntityManager(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    /**
     * @param scheduler sets the {@link #scheduler}
     */
    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }

    /**
     * Initialize reservation database.
     */
    public void init()
    {
        if (domain == null) {
            throw new IllegalStateException("Reservation database doesn't have the domain set!");
        }
        if (entityManager == null) {
            throw new IllegalStateException("Reservation database doesn't have the entity manager set!");
        }
        if (scheduler == null) {
            throw new IllegalStateException("Reservation database doesn't have the scheduler set!");
        }
        reservationRequestManager = new ReservationRequestManager(entityManager, scheduler);

        logger.debug("Checking reservation database...");

        reservationRequestManager.checkDomain(domain.getCodeName());
    }

    /**
     * Destroy reservation database.
     */
    public void destroy()
    {
        logger.debug("Closing reservation database...");
    }

    /**
     * Add new reservation request to the database.
     *
     * @param reservationRequest
     */
    public void addReservationRequest(ReservationRequest reservationRequest)
    {
        // Create new identifier for the reservation request
        if (reservationRequest.getIdentifier() != null) {
            throw new IllegalArgumentException("New reservation request should not have identifier filled ("
                    + reservationRequest.getIdentifier() + ")!");
        }
        reservationRequest.createNewIdentifier(domain.getCodeName());

        reservationRequestManager.create(reservationRequest);
    }

    /**
     * Update reservation request in the database.
     *
     * @param reservationRequest
     */
    public void updateReservationRequest(ReservationRequest reservationRequest)
    {
        reservationRequestManager.update(reservationRequest);
    }

    /**
     * Delete reservation request in the database
     *
     * @param reservationRequest
     */
    public void removeReservationRequest(ReservationRequest reservationRequest)
    {
        reservationRequestManager.delete(reservationRequest);
    }

    /**
     * @return list of all reservation requests in the database.
     */
    public List<ReservationRequest> listReservationRequests()
    {
        return reservationRequestManager.list();
    }
}
