package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.request.*;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a component for a domain controller that is started before scheduler.
 * The component process "not-preprocessed" reservation request and enumerate it
 * to compartment request that are scheduled by a scheduler.
 * <p/>
 * Without preprocessor, the scheduler doesn't have any input compartment request.
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

        logger.debug("Running preprocessor...");

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        // List all not preprocessed reservation requests
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        List<ReservationRequest> reservationRequests = reservationRequestManager.listNotPreprocessed(interval);

        for (ReservationRequest reservationRequest : reservationRequests) {
            synchronizeCompartmentRequests(reservationRequest, interval, entityManager);
        }

        entityManager.getTransaction().commit();
    }

    /**
     * Synchronize (create/modify/delete) compartment requests from a single persisted reservation request.
     */
    private void synchronizeCompartmentRequests(ReservationRequest reservationRequest, Interval interval,
            EntityManager entityManager)
    {
        reservationRequest.checkPersisted();

        CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(entityManager);

        // Get list of date/time slots
        List<Interval> slots = reservationRequest.enumerateRequestedSlots(interval);

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

        throw new RuntimeException("TODO: Modify reservation request state");
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
}
