package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.common.AbsoluteDateTimeSpecification;
import cz.cesnet.shongo.controller.request.Compartment;
import cz.cesnet.shongo.controller.request.CompartmentRequest;
import cz.cesnet.shongo.controller.request.CompartmentRequestManager;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents object that is responsible for scheduling resources to requested reservations.
 * <p/>
 * Scheduler runs in a time span for which it enumerates reservation requests to compartment requests.
 * Each compartment request must be filled in by additional information to become complete. Complete
 * compartment requests than can be scheduled - resources can be allocated.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Scheduler
{
    private static Logger logger = LoggerFactory.getLogger(Scheduler.class);

    /**
     * Entity manager that is used for loading/saving reservation requests.
     */
    private EntityManager entityManager;

    /**
     * @see CompartmentRequestManager
     */
    private CompartmentRequestManager compartmentRequestManager;

    /**
     * Current epoch that is considered by the scheduler.
     */
    private Epoch currentEpoch = new Epoch(null, null);

    /**
     * Constructor.
     */
    public Scheduler()
    {
    }

    /**
     * Constructor of scheduler.
     *
     * @param entityManager sets the {@link #entityManager}
     */
    public Scheduler(EntityManager entityManager)
    {
        setEntityManager(entityManager);
        init();
    }

    /**
     * Initialize reservation database.
     */
    public void init()
    {
        if (entityManager == null) {
            throw new IllegalStateException("Scheduler doesn't have the entity manager set!");
        }
        compartmentRequestManager = new CompartmentRequestManager(entityManager, this);

        logger.debug("Starting scheduler...");
    }

    /**
     * Destroy scheduler.
     */
    public void destroy()
    {
        logger.debug("Closing scheduler...");
    }

    /**
     * Run scheduler for a given epoch.
     *
     * @param epoch
     */
    public void run(Epoch epoch)
    {
        // TODO: Refactor scheduler to be able to run it periodically even for different epochs
    }

    /**
     * @param entityManager sets the {@link #entityManager}
     */
    public void setEntityManager(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    /**
     * @return {@link #currentEpoch}
     */
    public synchronized Epoch getCurrentEpoch()
    {
        if (currentEpoch == null) {
            throw new IllegalStateException("Cannot get current epoch because it is not set!");
        }
        return currentEpoch;
    }

    /**
     * Set current epoch.
     *
     * @param from
     * @param to
     */
    public synchronized void setCurrentEpoch(AbsoluteDateTimeSpecification from, AbsoluteDateTimeSpecification to)
    {
        currentEpoch = new Epoch(from, to);
    }

    public void onNewReservationRequest(ReservationRequest reservationRequest)
    {
        synchronizeCompartmentRequests(reservationRequest);
    }

    public void onUpdateReservationRequest(ReservationRequest reservationRequest)
    {
        synchronizeCompartmentRequests(reservationRequest);
    }

    public void onDeleteReservationRequest(ReservationRequest reservationRequest)
    {

    }

    public void onNewCompartmentRequest(CompartmentRequest compartmentRequest)
    {
    }

    public void onUpdateCompartmentRequest(CompartmentRequest compartmentRequest)
    {
    }

    public void onDeleteCompartmentRequest(CompartmentRequest compartmentRequest)
    {
    }

    /**
     * Synchronize (create/modify/delete) compartment requests from a single persisted reservation request.
     */
    private void synchronizeCompartmentRequests(ReservationRequest reservationRequest)
    {
        reservationRequest.checkPersisted();

        Epoch epoch = getCurrentEpoch();

        // Get list of date/time slots
        List<Interval> slots = reservationRequest.enumerateRequestedSlots(/*epoch.getFrom(), epoch.getTo()*/null, null);

        // Start transaction if is no active
        EntityTransaction transaction = null;
        if (entityManager.getTransaction().isActive() == false) {
            transaction = entityManager.getTransaction();
            transaction.begin();
        }

        // List all compartment requests for the reservation request
        List<CompartmentRequest> compartmentRequestList = compartmentRequestManager.list(reservationRequest);

        // Compartment requests are synchronized per compartment from reservation request
        for (Compartment compartment : reservationRequest.getRequestedCompartments()) {
            // List existing compartment requests for reservation request
            List<CompartmentRequest> requestListForCompartment = compartmentRequestManager.list(compartment);

            // Create map of compartment requests with date/time slot as key
            // and remove compartment request from list of all compartment request
            Map<Interval, CompartmentRequest> map = new HashMap<Interval, CompartmentRequest>();
            for (CompartmentRequest compartmentRequest : requestListForCompartment) {
                map.put(compartmentRequest.getRequestedSlot(), compartmentRequest);
                compartmentRequestList.remove(compartmentRequest);
            }

            // For each requested slot we must create or modify compartment request.
            // If we find date/time slot in prepared map we modify the corresponding request
            // and remove it from map, otherwise we create a new compartment request.
            for (Interval slot : slots) {
                // Modify existing compartment request
                if (map.containsKey(slot)) {
                    CompartmentRequest compartmentRequest = map.get(slot);
                    compartmentRequestManager.update(compartmentRequest, compartment);
                    map.remove(slot);
                }
                // Create new compartment request
                else {
                    compartmentRequestManager.create(compartment, slot);
                }
            }

            // All compartment requests that remains in map must be deleted
            for (CompartmentRequest compartmentRequest : map.values()) {
                compartmentRequestManager.delete(compartmentRequest);
            }
        }

        // All compartment requests that remains in list of all must be deleted
        for (CompartmentRequest compartmentRequest : compartmentRequestList) {
            compartmentRequestManager.delete(compartmentRequest);
        }

        // Commit transaction if was started
        if (transaction != null) {
            transaction.commit();
        }
    }
}
