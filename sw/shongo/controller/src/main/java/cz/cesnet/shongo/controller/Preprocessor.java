package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.cache.Cache;
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
     * @see {@link cz.cesnet.shongo.controller.cache.Cache}
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
        logger.debug("Running preprocessor for interval '{}'...", Temporal.formatInterval(interval));

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
            Reporter.reportInternalError(Reporter.PREPROCESSOR, exception);
        }
    }

    /**
     * Synchronize (create/modify/delete) {@link ReservationRequest}s from a single {@link ReservationRequestSet}.
     */
    private void processReservationRequestSet(ReservationRequestSet reservationRequestSet, Interval interval,
            EntityManager entityManager) throws Exception
    {
        reservationRequestSet.checkPersisted();

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
        PreprocessorStateManager stateManager = new PreprocessorStateManager(entityManager, reservationRequestSet);
        try {
            authorizationManager.beginTransaction(authorization);
            entityManager.getTransaction().begin();

            logger.info("Pre-processing reservation request '{}'...", reservationRequestSet.getId());

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

            // Reservation requests are synchronized per specification from the set
            Specification specification = reservationRequestSet.getSpecification();
            if (specification == null) {
                throw new RuntimeException("Specification should not be null!");
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

            // New reservation requests for creating ACL records
            Collection<ReservationRequest> newReservationRequests = new LinkedList<ReservationRequest>();

            // For each requested slot we must create or modify reservation request.
            // If we find date/time slot in prepared map we modify the corresponding request
            // and remove it from map, otherwise we create a new reservation request.
            for (Interval slot : slots) {
                ReservationRequest reservationRequest;
                // Modify existing reservation request
                if (map.containsKey(slot)) {
                    reservationRequest = map.get(slot);
                    boolean modified = reservationRequest.synchronizeFrom(reservationRequestSet,
                            reservationRequestSet.getOriginalSpecifications());
                    // Reservation request should be allocated when it was modified or
                    // when the reservation request set was modified, ie., it is not preprocessed in the slot
                    if (modified || stateManager.getState(slot).equals(PreprocessorState.NOT_PREPROCESSED)) {
                        // Reservation request was modified, so we must clear it's state
                        reservationRequest.clearState();
                    }

                    // Remove the slot from the map for the corresponding reservation request to not be deleted
                    map.remove(slot);
                }
                // Create new reservation request
                else {
                    reservationRequest = new ReservationRequest();
                    reservationRequest.setSlot(slot);
                    reservationRequest.synchronizeFrom(reservationRequestSet,
                            reservationRequestSet.getOriginalSpecifications());
                    reservationRequestManager.create(reservationRequest);

                    reservationRequestSet.addReservationRequest(reservationRequest);

                    newReservationRequests.add(reservationRequest);
                }
                reservationRequest.updateStateBySpecification();
            }

            // All reservation requests that remains in map must be deleted
            for (ReservationRequest reservationRequest : map.values()) {
                reservationRequests.remove(reservationRequest);
                reservationRequestSet.removeReservationRequest(reservationRequest);

                // Delete ACL records
                reservationRequestManager.hardDelete(reservationRequest, authorizationManager);
            }

            // All reservation requests that remains in list of all must be deleted
            for (ReservationRequest reservationRequest : reservationRequests) {
                reservationRequestSet.removeReservationRequest(reservationRequest);

                // Delete ACL records
                reservationRequestManager.hardDelete(reservationRequest, authorizationManager);
            }

            // Update reservation request
            reservationRequestManager.update(reservationRequestSet, false);

            // When the reservation request hasn't got any future requested slot, the preprocessed state
            // is until "infinite".
            if (!reservationRequestSet.hasSlotAfter(interval.getEnd())) {
                interval = new Interval(interval.getStart(), PreprocessorStateManager.MAXIMUM_INTERVAL_END);
            }

            // Set state preprocessed state for the interval to reservation request
            stateManager.setState(PreprocessorState.PREPROCESSED, interval);

            // Create ACL records
            for (ReservationRequest reservationRequest : newReservationRequests) {
                authorizationManager.createAclRecordsForChildEntity(reservationRequestSet, reservationRequest);
            }

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();
        }
        catch (Exception exception) {
            if (authorizationManager.isTransactionActive()) {
                authorizationManager.rollbackTransaction();
            }
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw exception;
        }
    }
}
