package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.TransactionHelper;
import cz.cesnet.shongo.controller.api.ControllerFault;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.util.TemporalHelper;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Represents a {@link Component} that is responsible for enumerating {@link ReservationRequestSet}s
 * to {@link ReservationRequest}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Preprocessor extends Component
{
    private static Logger logger = LoggerFactory.getLogger(Preprocessor.class);

    /**
     * Run preprocessor for a given interval.
     *
     * @param interval
     */
    public void run(Interval interval, EntityManager entityManager) throws FaultException
    {
        logger.info("Running preprocessor for interval '{}'...", TemporalHelper.formatInterval(interval));

        TransactionHelper.Transaction transaction = TransactionHelper.beginTransaction(entityManager);

        try {
            // List all not preprocessed reservation request sets
            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            List<ReservationRequestSet> reservationRequestSets =
                    reservationRequestManager.listNotPreprocessedReservationRequestSets(interval);

            // Process all reservation request sets
            for (ReservationRequestSet reservationRequestSet : reservationRequestSets) {
                processReservationRequestSet(reservationRequestSet, interval, entityManager);
            }

            transaction.commit();
        }
        catch (Exception exception) {
            transaction.rollback();
            throw new FaultException(exception, ControllerFault.PREPROCESSOR_FAILED);
        }
    }

    /**
     * Run preprocessor only for single {@link ReservationRequestSet} with given identifier for a given interval.
     *
     * @param reservationRequestSetId
     * @param interval
     * @param entityManager
     */
    public void run(long reservationRequestSetId, Interval interval, EntityManager entityManager) throws FaultException
    {
        logger.info("Running preprocessor for a single reservation request set '{}' for interval '{}'...",
                reservationRequestSetId, TemporalHelper.formatInterval(interval));

        TransactionHelper.Transaction transaction = TransactionHelper.beginTransaction(entityManager);

        try {
            // Get reservation request set by identifier
            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            ReservationRequestSet reservationRequestSet =
                    reservationRequestManager.getReservationRequestSet(reservationRequestSetId);

            if (reservationRequestSet == null) {
                throw new IllegalArgumentException(String.format("Reservation request set '%s' doesn't exist!",
                        reservationRequestSetId));
            }
            ReservationRequestSetStateManager reservationRequestStateManager =
                    new ReservationRequestSetStateManager(entityManager, reservationRequestSet);
            if (reservationRequestStateManager.getState(interval) != ReservationRequestSet.State.NOT_PREPROCESSED) {
                throw new IllegalStateException(String.format(
                        "Reservation request set '%s' is already preprocessed in %s!",
                        reservationRequestSetId, interval));
            }
            processReservationRequestSet(reservationRequestSet, interval, entityManager);

            transaction.commit();
        }
        catch (Exception exception) {
            transaction.rollback();
            throw new FaultException(exception, ControllerFault.PREPROCESSOR_FAILED);
        }
    }

    /**
     * Synchronize (create/modify/delete) {@link ReservationRequest}s from a single {@link ReservationRequestSet}.
     */
    private void processReservationRequestSet(ReservationRequestSet reservationRequestSet, Interval interval,
            EntityManager entityManager)
    {
        reservationRequestSet.checkPersisted();

        logger.info("Pre-processing reservation request '{}'...", reservationRequestSet.getId());

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        // Get list of date/time slots
        List<Interval> slots = reservationRequestSet.enumerateRequestedSlots(interval);

        // List all reservation requests for the set
        List<ReservationRequest> reservationRequests =
                reservationRequestManager.listReservationRequestsBySet(reservationRequestSet, interval);

        // Build map of reservation requests by original specification
        Map<Specification, Set<ReservationRequest>> reservationRequestsByOriginalSpecification =
                new HashMap<Specification, Set<ReservationRequest>>();
        for (ReservationRequest reservationRequest : reservationRequests) {
            Specification originalSpecification = reservationRequestSet.getOriginalSpecifications()
                    .get(reservationRequest.getSpecification());
            if (originalSpecification == null) {
                originalSpecification = reservationRequest.getSpecification();
            }
            Set<ReservationRequest> set = reservationRequestsByOriginalSpecification.get(originalSpecification);
            if (set == null) {
                set = new HashSet<ReservationRequest>();
                reservationRequestsByOriginalSpecification.put(originalSpecification, set);
            }
            set.add(reservationRequest);
        }

        // Build set of existing specifications for the set
        Set<Long> specifications = new HashSet<Long>();

        // Reservation requests are synchronized per specification from the set
        for (Specification specification : reservationRequestSet.getSpecifications()) {
            if (specification == null) {
                throw new IllegalStateException("Specification should not be null!");
            }
            // List existing reservation requests for the set in the interval
            Set<ReservationRequest> reservationRequestsForSpecification =
                    reservationRequestsByOriginalSpecification.get(specification);

            // Create map of reservation requests with date/time slot as key
            // and remove reservation request from list of all reservation request
            Map<Interval, ReservationRequest> map = new HashMap<Interval, ReservationRequest>();
            if (reservationRequestsForSpecification != null) {
                for (ReservationRequest reservationRequest : reservationRequestsForSpecification) {
                    map.put(reservationRequest.getRequestedSlot(), reservationRequest);
                    reservationRequests.remove(reservationRequest);
                }
            }

            // For each requested slot we must create or modify reservation request.
            // If we find date/time slot in prepared map we modify the corresponding request
            // and remove it from map, otherwise we create a new reservation request.
            for (Interval slot : slots) {
                ReservationRequest reservationRequest;
                // Modify existing reservation request
                if (map.containsKey(slot)) {
                    reservationRequest = map.get(slot);
                    if ( updateReservationRequest(reservationRequest, reservationRequestSet, specification) ) {
                        // Reservation request was modified, so we must clear it's state
                        reservationRequest.clearState();
                        // And if allocated reservation exists, we remove reference to it and it will be deleted
                        // at the start of the Scheduler
                        reservationRequest.setReservation(null);
                    }

                    // Remove the slot from the map for the corresponding reservation request to not be deleted
                    map.remove(slot);
                }
                // Create new reservation request
                else {
                    reservationRequest = new ReservationRequest();
                    reservationRequest.setRequestedSlot(slot);
                    updateReservationRequest(reservationRequest, reservationRequestSet, specification);
                    reservationRequestSet.addReservationRequest(reservationRequest);
                }
                reservationRequest.updateStateBySpecifications();
            }

            // All reservation requests that remains in map must be deleted
            for (ReservationRequest reservationRequest : map.values()) {
                reservationRequestSet.removeReservationRequest(reservationRequest);
                reservationRequestManager.delete(reservationRequest);
            }

            specifications.add(specification.getId());
        }

        // All reservation requests that remains in list of all must be deleted
        for (ReservationRequest reservationRequest : reservationRequests) {
            reservationRequestSet.removeReservationRequest(reservationRequest);
            reservationRequestManager.delete(reservationRequest);

            // If referenced specification isn't in reservation request any more
            Specification specification = reservationRequest.getSpecification();
            if (!specifications.contains(specification)) {
                // Remove the specification too
                entityManager.remove(specification);
            }
        }

        // Update reservation request
        reservationRequestManager.update(reservationRequestSet);

        // When the reservation request hasn't got any future requested slot, the preprocessed state
        // is until "infinite".
        if (!reservationRequestSet.hasRequestedSlotAfter(interval.getEnd())) {
            interval = new Interval(interval.getStart(), ReservationRequestSetStateManager.MAXIMUM_INTERVAL_END);
        }

        // Set state preprocessed state for the interval to reservation request
        ReservationRequestSetStateManager.setState(entityManager, reservationRequestSet,
                ReservationRequestSet.State.PREPROCESSED, interval);
    }

    /**
     * Update given {@code specification} according to given {@code specificationFrom}.
     *
     * @param specification
     * @param specificationFrom
     * @param reservationRequestSet
     * @return true if some change(s) were made in the {@link Specification}
     *         false otherwise
     */
    private static boolean updateSpecification(Specification specification, Specification specificationFrom,
            ReservationRequestSet reservationRequestSet)
    {
        boolean modified = false;
        modified |= specification.synchronizeFrom(specificationFrom);
        if (specification instanceof CompositeSpecification) {
            CompositeSpecification compositeSpecification = (CompositeSpecification) specification;
            CompositeSpecification compositeSpecificationFrom = (CompositeSpecification) specificationFrom;

            // Get map of original specifications by cloned specifications
            Map<Specification, Specification> originalSpecifications = reservationRequestSet
                    .getOriginalSpecifications();

            // Build set of new specifications
            Set<Specification> newSpecifications = new HashSet<Specification>();
            for (Specification newSpecification : compositeSpecificationFrom.getSpecifications()) {
                newSpecifications.add(newSpecification);
            }

            // Update or delete child specifications
            Set<Specification> deletedSpecifications = new HashSet<Specification>();
            for (Specification childSpecification : compositeSpecification.getSpecifications()) {
                Specification originalSpecification = originalSpecifications.get(childSpecification);
                if (originalSpecification == null) {
                    originalSpecification = childSpecification;
                }
                if (newSpecifications.contains(originalSpecification)) {
                    modified |= updateSpecification(childSpecification, originalSpecification, reservationRequestSet);
                    newSpecifications.remove(originalSpecification);
                }
                else {
                    deletedSpecifications.add(childSpecification);
                }
            }
            for (Specification deletedSpecification : deletedSpecifications) {
                compositeSpecification.removeSpecification(deletedSpecification);
                modified = true;
            }

            // Add new child specifications
            for (Specification newSpecification : newSpecifications) {
                if (newSpecification instanceof StatefulSpecification) {
                    StatefulSpecification newStatefulSpecification = (StatefulSpecification) newSpecification;
                    compositeSpecification.addSpecification(newStatefulSpecification.clone(originalSpecifications));
                }
                else {
                    compositeSpecification.addSpecification(newSpecification);
                }
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Update given {@code reservationRequest} according to given {@code specification}.
     *
     * @param reservationRequest
     * @param specification
     * @return true if some change(s) were made in the {@link ReservationRequest}
     *         false otherwise
     */
    private static boolean updateReservationRequest(ReservationRequest reservationRequest,
            ReservationRequestSet reservationRequestSet, Specification specification)
    {
        // Tracks whether the reservation request was modified
        boolean modified = false;
        modified |= reservationRequest.synchronizeFrom(reservationRequestSet);

        Specification oldSpecification = reservationRequest.getSpecification();
        if (oldSpecification == null || oldSpecification.getClass() != specification.getClass()) {
            // Setup new specification
            if (specification instanceof StatefulSpecification) {
                StatefulSpecification statefulSpecification = (StatefulSpecification) specification;
                reservationRequest.setSpecification(
                        statefulSpecification.clone(reservationRequestSet.getOriginalSpecifications()));
            }
            else {
                reservationRequest.setSpecification(specification);
            }
            modified = true;
        }
        else {
            // Check specification for modifications
            modified |= updateSpecification(oldSpecification, specification, reservationRequestSet);
        }

        return modified;
    }

    /**
     * Run preprocessor on given {@code entityManager} and interval.
     *
     * @param entityManager
     * @param interval
     */
    public static void createAndRun(Interval interval, EntityManager entityManager) throws FaultException
    {
        Preprocessor preprocessor = new Preprocessor();
        preprocessor.init();
        preprocessor.run(interval, entityManager);
        preprocessor.destroy();
    }

    /**
     * Run preprocessor on given {@code entityManager}, for a single reservation request and given interval.
     *
     * @param entityManager
     * @param reservationRequestId
     * @param interval
     */
    public static void createAndRun(long reservationRequestId, Interval interval, EntityManager entityManager)
            throws FaultException
    {
        Preprocessor preprocessor = new Preprocessor();
        preprocessor.init();
        preprocessor.run(reservationRequestId, interval, entityManager);
        preprocessor.destroy();
    }
}
