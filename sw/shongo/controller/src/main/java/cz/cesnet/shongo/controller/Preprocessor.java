package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.authorization.AclRecord;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.request.*;
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
public class Preprocessor extends Component implements Component.AuthorizationAware
{
    private static Logger logger = LoggerFactory.getLogger(Preprocessor.class);

    /**
     * @see {@link Cache}
     */
    private Cache cache;

    /**
     * @see Authorization
     */
    private Authorization authorization;

    /**
     * @param cache sets the {@link #cache}
     */
    public void setCache(Cache cache)
    {
        this.cache = cache;
    }

    @Override
    public void setAuthorization(Authorization authorization)
    {
        this.authorization = authorization;
    }

    @Override
    public void init(Configuration configuration)
    {
        checkDependency(cache, Cache.class);
        super.init(configuration);
    }

    /**
     * Run preprocessor for a given interval.
     *
     * @param interval
     */
    public synchronized void run(Interval interval, EntityManager entityManager)
    {
        logger.info("Running preprocessor for interval '{}'...", Temporal.formatInterval(interval));

        try {
            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

            // Process all not-preprocessed reservation request sets
            List<ReservationRequestSet> reservationRequestSets =
                    reservationRequestManager.listNotPreprocessedReservationRequestSets(interval);
            for (ReservationRequestSet reservationRequestSet : reservationRequestSets) {
                processReservationRequestSet(reservationRequestSet, interval, entityManager);
            }
        }
        catch (Exception exception) {
            throw new IllegalStateException("Preprocessor failed", exception);
        }
    }

    /**
     * Synchronize (create/modify/delete) {@link ReservationRequest}s from a single {@link ReservationRequestSet}.
     */
    private void processReservationRequestSet(ReservationRequestSet reservationRequestSet, Interval interval,
            EntityManager entityManager) throws Exception
    {
        // Store created and deleted reservation requests for updating ACL
        Set<ReservationRequest> createdReservationRequests = new HashSet<ReservationRequest>();
        Set<AclRecord> aclRecordsToDelete = new HashSet<AclRecord>();

        reservationRequestSet.checkPersisted();
        try {
            entityManager.getTransaction().begin();

            logger.info("Pre-processing reservation request '{}'...", reservationRequestSet.getId());

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

            // Get list of date/time slots
            Collection<Interval> slots = reservationRequestSet.enumerateSlots(interval);

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
            Specification specification = reservationRequestSet.getSpecification();
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
                    map.put(reservationRequest.getSlot(), reservationRequest);
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
                    if (updateReservationRequest(reservationRequest, reservationRequestSet, specification)) {
                        // Reservation request was modified, so we must clear it's state
                        reservationRequest.clearState();
                    }

                    // Remove the slot from the map for the corresponding reservation request to not be deleted
                    map.remove(slot);
                }
                // Create new reservation request
                else {
                    reservationRequest = new ReservationRequest();
                    reservationRequest.setUserId(reservationRequestSet.getUserId());
                    reservationRequest.setSlot(slot);
                    updateReservationRequest(reservationRequest, reservationRequestSet, specification);
                    reservationRequestSet.addReservationRequest(reservationRequest);

                    // Remember for updating ACL
                    createdReservationRequests.add(reservationRequest);
                }
                reservationRequest.updateStateBySpecification();
            }

            // All reservation requests that remains in map must be deleted
            for (ReservationRequest reservationRequest : map.values()) {
                reservationRequestSet.removeReservationRequest(reservationRequest);
                reservationRequestManager.delete(reservationRequest);

                // Remember for updating ACL
                if (authorization != null) {
                    aclRecordsToDelete.addAll(authorization.getAclRecordsForDeletion(reservationRequest));
                }
            }

            // All reservation requests that remains in list of all must be deleted
            for (ReservationRequest reservationRequest : reservationRequests) {
                reservationRequestSet.removeReservationRequest(reservationRequest);
                reservationRequestManager.delete(reservationRequest);

                // Remember for updating ACL
                if (authorization != null) {
                    aclRecordsToDelete.addAll(authorization.getAclRecordsForDeletion(reservationRequest));
                }
            }

            // Update reservation request
            reservationRequestManager.update(reservationRequestSet, false);

            // When the reservation request hasn't got any future requested slot, the preprocessed state
            // is until "infinite".
            if (!reservationRequestSet.hasSlotAfter(interval.getEnd())) {
                interval = new Interval(interval.getStart(), PreprocessorStateManager.MAXIMUM_INTERVAL_END);
            }

            // Set state preprocessed state for the interval to reservation request
            PreprocessorStateManager.setState(entityManager, reservationRequestSet,
                    PreprocessorState.PREPROCESSED, interval);

            entityManager.getTransaction().commit();
        }
        catch (Exception exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw exception;
        }

        // Update ACL
        if (authorization != null) {
            authorization.deleteAclRecords(aclRecordsToDelete);
            for (ReservationRequest reservationRequest : createdReservationRequests) {
                authorization.createAclRecordsForChildEntity(reservationRequestSet, reservationRequest);
            }
        }
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
            for (Specification newSpecification : compositeSpecificationFrom.getChildSpecifications()) {
                newSpecifications.add(newSpecification);
            }

            // Update or delete child specifications
            Set<Specification> deletedSpecifications = new HashSet<Specification>();
            for (Specification childSpecification : compositeSpecification.getChildSpecifications()) {
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
                compositeSpecification.removeChildSpecification(deletedSpecification);
                modified = true;
            }

            // Add new child specifications
            for (Specification newSpecification : newSpecifications) {
                compositeSpecification.addChildSpecification(newSpecification.clone(originalSpecifications));
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
            reservationRequest.setSpecification(specification.clone(reservationRequestSet.getOriginalSpecifications()));
            modified = true;
        }
        else {
            // Check specification for modifications
            modified |= updateSpecification(oldSpecification, specification, reservationRequestSet);
        }

        return modified;
    }
}
