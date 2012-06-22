package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.request.*;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Represents a component for a domain controller that is started before scheduler.
 * The component process "not-preprocessed" reservation requests and enumerate them
 * to compartment requests that are scheduled by a scheduler.
 * <p/>
 * Without preprocessor, the scheduler doesn't have any input compartment requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Preprocessor extends Component
{
    private static Logger logger = LoggerFactory.getLogger(Preprocessor.class);

    @Override
    public void init()
    {
        super.init();
    }

    @Override
    public void destroy()
    {
        super.destroy();
    }

    /**
     * Run preprocessor for a given interval.
     *
     * @param interval
     */
    public void run(Interval interval)
    {
        checkInitialized();

        logger.info("Running preprocessor...");

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        // List all not preprocessed reservation requests
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        List<ReservationRequest> reservationRequests = reservationRequestManager.listNotPreprocessed(interval);

        // Process all reservation requests
        for (ReservationRequest reservationRequest : reservationRequests) {
            processReservationRequest(reservationRequest, interval, entityManager);
        }

        entityManager.getTransaction().commit();
        entityManager.close();
    }

    /**
     * Run preprocessor only for single reservation request with given identifier for a given interval.
     *
     * @param reservationRequestId
     * @param interval
     */
    public void run(long reservationRequestId, Interval interval)
    {
        checkInitialized();

        logger.info("Running preprocessor for a single reservation request '{}'...", reservationRequestId);

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        // Get reservation request by identifier
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ReservationRequest reservationRequest = reservationRequestManager.get(reservationRequestId);

        if ( reservationRequest == null ) {
            throw new IllegalArgumentException(String.format("Reservation request '%s' doesn't exist!",
                    reservationRequestId));
        }
        ReservationRequestStateManager reservationRequestStateManager =
                new ReservationRequestStateManager(entityManager, reservationRequest);
        if ( reservationRequestStateManager.getState(interval) != ReservationRequest.State.NOT_PREPROCESSED) {
            throw new IllegalStateException(String.format("Reservation request '%s' is already preprocessed in %s!",
                    reservationRequestId, interval));
        }
        processReservationRequest(reservationRequest, interval, entityManager);

        entityManager.getTransaction().commit();
        entityManager.close();
    }

    /**
     * Synchronize (create/modify/delete) compartment requests from a single persisted reservation request.
     */
    private void processReservationRequest(ReservationRequest reservationRequest, Interval interval,
            EntityManager entityManager)
    {
        reservationRequest.checkPersisted();

        logger.info("Preprocessing reservation request '{}'...", reservationRequest.getId());

        CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(entityManager);

        // Get list of date/time slots
        List<Interval> slots = reservationRequest.enumerateRequestedSlots(interval);

        // List all compartment requests for the reservation request
        List<CompartmentRequest> compartmentRequestList = compartmentRequestManager.listByReservationRequest(
                reservationRequest, interval);

        // Build set of existing compartments for the reservation request
        Set<Long> compartmentSet = new HashSet<Long>();

        // Compartment requests are synchronized per compartment from reservation request
        for (Compartment compartment : reservationRequest.getRequestedCompartments()) {
            // List existing compartment requests for reservation request in the interval
            List<CompartmentRequest> requestListForCompartment = compartmentRequestManager.listByCompartment(
                    compartment, interval);

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

            compartmentSet.add(compartment.getId());
        }

        // All compartment requests that remains in list of all must be deleted
        for (CompartmentRequest compartmentRequest : compartmentRequestList) {
            compartmentRequestManager.delete(compartmentRequest);

            // If referenced compartment isn't in reservation request any more
            Compartment compartment = compartmentRequest.getCompartment();
            if (!compartmentSet.contains(compartment)) {
                // Remove the compartment too
                entityManager.remove(compartment);
            }
        }

        // When the reservation request hasn't got any future requested slot, the preprocessed state
        // is until "infinite".
        if ( !reservationRequest.hasRequestedSlotAfter(interval.getEnd()) ) {
            interval = new Interval(interval.getStart(), ReservationRequestStateManager.MAXIMUM_INTERVAL_END);
        }

        // Set state preprocessed state for the interval to reservation request
        ReservationRequestStateManager.setState(entityManager, reservationRequest,
                ReservationRequest.State.PREPROCESSED, interval);
    }

    /**
     * Run preprocessor on given entityManagerFactory and interval.
     *
     * @param entityManagerFactory
     * @param interval
     */
    public static void run(EntityManagerFactory entityManagerFactory, Interval interval)
    {
        Preprocessor preprocessor = new Preprocessor();
        preprocessor.setEntityManagerFactory(entityManagerFactory);
        preprocessor.init();
        preprocessor.run(interval);
        preprocessor.destroy();
    }

    /**
     * Run preprocessor on given entityManagerFactory, for a single reservation request and given interval.
     *
     * @param entityManagerFactory
     * @param reservationRequestId
     * @param interval
     */
    public static void run(EntityManagerFactory entityManagerFactory, long reservationRequestId,
            Interval interval)
    {
        Preprocessor preprocessor = new Preprocessor();
        preprocessor.setEntityManagerFactory(entityManagerFactory);
        preprocessor.init();
        preprocessor.run(reservationRequestId, interval);
        preprocessor.destroy();
    }
}
