package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
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
        // Round interval start to whole hours (otherwise the reservation requests with future date/time slots
        // would be always processed, because the interval would keep changing all the time)
        DateTime intervalStart = interval.getStart();
        intervalStart = intervalStart.withField(DateTimeFieldType.minuteOfHour(), 0);
        intervalStart = intervalStart.withField(DateTimeFieldType.secondOfMinute(), 0);
        intervalStart = intervalStart.withField(DateTimeFieldType.millisOfSecond(), 0);
        interval = new Interval(intervalStart, interval.toPeriod());

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
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        PreprocessorStateManager stateManager = new PreprocessorStateManager(entityManager, reservationRequestSet);
        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            logger.info("Pre-processing reservation request '{}'...", reservationRequestSet.getId());

            // Get allocation for the reservation request set
            Allocation allocation = reservationRequestSet.getAllocation();

            // List all child reservation requests for the set
            List<ReservationRequest> childReservationRequests =
                    reservationRequestManager.listChildReservationRequests(reservationRequestSet, interval);

            // For each requested slot we must create or modify reservation request.
            // If we find date/time slot in prepared map we modify the corresponding request
            // and remove it from map, otherwise we create a new reservation request.
            for (Interval slot : reservationRequestSet.enumerateSlots(interval)) {
                // Find existing child reservation request by date/time slot
                ReservationRequest childReservationRequest = null;
                Interval childReservationRequestOverlap = null;
                for (ReservationRequest possibleChildReservationRequest : childReservationRequests) {
                    Interval possibleOverlap = slot.overlap(possibleChildReservationRequest.getSlot());
                    // If possible child reservation request overlaps current slot
                    if (possibleOverlap != null) {
                        if (childReservationRequest != null) {
                            // If possible child reservation has already been found and new one does not fit better
                            long millisOld = childReservationRequestOverlap.toDurationMillis();
                            long millisNew = possibleOverlap.toDurationMillis();
                            if (millisNew <= millisOld) {
                                // Skip the new one
                                continue;
                            }
                        }
                        // Existing child reservation request was found
                        childReservationRequest = possibleChildReservationRequest;
                        childReservationRequestOverlap = possibleOverlap;
                    }
                }

                // Modify existing reservation request
                if (childReservationRequest != null) {
                    // When the parent reservation request is not preprocessed in the slot
                    if (stateManager.getState(slot).equals(PreprocessorState.NOT_PREPROCESSED)) {
                        // Update child reservation request
                        boolean modified = childReservationRequest.synchronizeFrom(reservationRequestSet);

                        // Update child reservation request date/time slot
                        if (!slot.equals(childReservationRequest.getSlot())) {
                            childReservationRequest.setSlot(slot);
                            modified = true;
                        }

                        // When the child reservation request was modified it should be (re)allocated
                        if (modified ) {
                            // We must reallocate child reservation request, so clear it's state
                            childReservationRequest.clearState();
                        }
                    }

                    // Remove the child reservation request from the list to not be deleted
                    childReservationRequests.remove(childReservationRequest);
                }
                else {
                    // Create a new reservation request
                    childReservationRequest = new ReservationRequest();
                    childReservationRequest.setSlot(slot);
                    childReservationRequest.synchronizeFrom(reservationRequestSet);
                    reservationRequestManager.create(childReservationRequest);

                    // Add the new reservation request as child to allocation
                    allocation.addChildReservationRequest(childReservationRequest);

                    // Create ACL records for the new reservation request
                    authorizationManager.createAclRecordsForChildEntity(reservationRequestSet, childReservationRequest);
                }

                // Update state for modified/new reservation request
                childReservationRequest.updateStateBySpecification();
            }

            // All child reservation requests that remains in list must be deleted
            for (ReservationRequest reservationRequest : childReservationRequests) {
                // Remove child reservation request from allocation
                allocation.removeChildReservationRequest(reservationRequest);

                // Delete child reservation request and all it's ACL records
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
