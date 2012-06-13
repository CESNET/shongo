package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.common.AbsoluteDateTimeSlot;
import cz.cesnet.shongo.common.Identifier;
import cz.cesnet.shongo.common.Person;
import cz.cesnet.shongo.controller.Domain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * @see ReservationRequestManager
     */
    private ReservationRequestManager reservationRequestManager;

    /**
     * @see CompartmentRequestManager
     */
    private CompartmentRequestManager compartmentRequestManager;

    /**
     * Constructor of reservation database.
     */
    public ReservationDatabase()
    {
    }

    /**
     * Constructor of reservation database.
     *
     * @param domain        sets the {@link #domain}
     * @param entityManager Sets the {@link #entityManager}
     */
    public ReservationDatabase(Domain domain, EntityManager entityManager)
    {
        setDomain(domain);
        setEntityManager(entityManager);
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
        reservationRequestManager = ReservationRequestManager.createInstance(entityManager);
        compartmentRequestManager = CompartmentRequestManager.createInstance(entityManager);

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

        entityManager.getTransaction().begin();
        reservationRequestManager.create(reservationRequest);
        entityManager.getTransaction().commit();
    }

    /**
     * Update reservation request in the database.
     *
     * @param reservationRequest
     */
    public void updateReservationRequest(ReservationRequest reservationRequest)
    {
        entityManager.getTransaction().begin();
        reservationRequestManager.update(reservationRequest);
        entityManager.getTransaction().commit();
    }

    /**
     * Delete reservation request in the database
     *
     * @param reservationRequest
     */
    public void removeReservationRequest(ReservationRequest reservationRequest)
    {
        entityManager.getTransaction().begin();
        reservationRequestManager.delete(reservationRequest);
        entityManager.getTransaction().commit();
    }

    /**
     * @return list of all reservation requests in the database.
     */
    public List<ReservationRequest> listReservationRequests()
    {
        return reservationRequestManager.list();
    }

    /**
     * @param identifier
     * @return {@link ReservationRequest} with given identifier or null if the request not exists
     */
    public ReservationRequest getReservationRequest(Identifier identifier)
    {
        return reservationRequestManager.get(identifier);
    }

    /**
     * @param reservationRequestIdentifier
     * @return list of existing compartment requests for a {@link ReservationRequest} with the given identifier
     */
    public List<CompartmentRequest> listCompartmentRequests(Identifier reservationRequestIdentifier)
    {
        return compartmentRequestManager.list(reservationRequestIdentifier);
    }
}
