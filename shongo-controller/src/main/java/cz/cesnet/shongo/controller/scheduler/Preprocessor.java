package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.SwitchableComponent;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.request.*;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Hours;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a {@link cz.cesnet.shongo.controller.Component} that is responsible for enumerating {@link cz.cesnet.shongo.controller.booking.request.ReservationRequestSet}s
 * to {@link ReservationRequest}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Preprocessor extends SwitchableComponent implements Component.AuthorizationAware, Component.NotificationManagerAware
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
     * @see NotificationManager
     */
    private NotificationManager notificationManager;

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
    public void setNotificationManager(NotificationManager notificationManager)
    {
        this.notificationManager = notificationManager;
    }

    @Override
    public void init(ControllerConfiguration configuration)
    {
        checkDependency(cache, Cache.class);
        super.init(configuration);
    }

    /**
     * Run preprocessor for a given interval.
     *
     * @param interval
     */
    public synchronized Result run(Interval interval, EntityManager entityManager)
    {
        Result result = new Result();
        if (!isEnabled()) {
            logger.warn("Skipping preprocessor because it is disabled...");
            return result;
        }

        cz.cesnet.shongo.util.Timer timer = new cz.cesnet.shongo.util.Timer();
        timer.start();

        // Round interval start to whole hours (otherwise the reservation requests with future date/time slots
        // would be always processed, because the interval would keep changing all the time)
        DateTime intervalStart = interval.getStart();
        intervalStart = intervalStart.withField(DateTimeFieldType.minuteOfHour(), 0);
        intervalStart = intervalStart.withField(DateTimeFieldType.secondOfMinute(), 0);
        intervalStart = intervalStart.withField(DateTimeFieldType.millisOfSecond(), 0);
        interval = new Interval(intervalStart, interval.toPeriod());

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.getInstance(DateTimeFormatter.Type.LONG);
        logger.debug("Running preprocessor for interval '{}'...", dateTimeFormatter.formatInterval(interval));

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        try {

            // Process all not-preprocessed reservation request sets
            List<ReservationRequestSet> reservationRequestSets =
                    reservationRequestManager.listNotPreprocessedReservationRequestSets(interval);
            for (ReservationRequestSet reservationRequestSet : reservationRequestSets) {
                processReservationRequestSet(reservationRequestSet, interval, entityManager, result);
            }

            if (!result.isEmpty()) {
                logger.info("Pre-processing done in {} ms (created: {}, modified: {}, deleted: {}).", new Object[]{
                        timer.stop(), result.createdReservationRequests, result.modifiedReservationRequests,
                        result.deletedReservationRequests
                });
            }

        }
        catch (Exception exception) {
            Reporter.getInstance().reportInternalError(Reporter.PREPROCESSOR, exception);
        }
        return result;
    }

    /**
     * Synchronize (create/modify/delete) {@link ReservationRequest}s from a single {@link ReservationRequestSet}.
     */
    private void processReservationRequestSet(ReservationRequestSet reservationRequestSet, Interval interval,
            EntityManager entityManager, Result result) throws Exception
    {
        reservationRequestSet.checkPersisted();

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        PreprocessorStateManager stateManager = new PreprocessorStateManager(entityManager, reservationRequestSet);
        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            logger.debug("Pre-processing reservation request '{}'...", reservationRequestSet.getId());

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
                int childReservationRequestHours = 0;
                for (ReservationRequest possibleChildReservationRequest : childReservationRequests) {
                    int possibleHours = Math.abs(Hours.hoursBetween(
                            slot.getStart(), possibleChildReservationRequest.getSlot().getStart()).getHours());
                    // If possible child reservation request overlaps current slot
                    if (possibleHours < 24) {
                        if (childReservationRequest != null) {
//                            int hours = Math.abs(Hours.hoursBetween(
//                                    slot.getStart(), possibleChildReservationRequest.getSlot().getEnd()).getHours());
                            // If possible child reservation has already been found and new one does not fit better
                            if (possibleHours >= childReservationRequestHours) {
                                // Skip the new one
                                continue;
                            }
                        }
                        // Existing child reservation request was found
                        childReservationRequest = possibleChildReservationRequest;
                        childReservationRequestHours = possibleHours;
                    }
                }

                // Modify existing reservation request
                if (childReservationRequest != null) {
                    // When the parent reservation request is not preprocessed in the slot
                    if (stateManager.getState(slot).equals(PreprocessorState.NOT_PREPROCESSED)) {
                        // Update child reservation request
                        boolean modified = childReservationRequest.synchronizeFrom(reservationRequestSet, entityManager);

                        // Update child reservation request date/time slot
//                        if (!slot.equals(childReservationRequest.getSlot())) {
                        if (!Temporal.isIntervalEqualed(slot, childReservationRequest.getSlot())) {
                            childReservationRequest.setSlot(slot);
                            modified = true;
                        }

                        // When the child reservation request was modified it should be (re)allocated
                        if (modified) {
                            // We must reallocate child reservation request, so clear it's state
                            childReservationRequest.clearState();

                            result.modifiedReservationRequests++;
                        }
                    }

                    // Remove the child reservation request from the list to not be deleted
                    childReservationRequests.remove(childReservationRequest);
                }
                else {
                    // Create a new reservation request
                    childReservationRequest = new ReservationRequest();
                    childReservationRequest.setSlot(slot);
                    childReservationRequest.synchronizeFrom(reservationRequestSet, entityManager);
                    reservationRequestManager.create(childReservationRequest);

                    childReservationRequest.getSpecification().updateSpecificationSummary(entityManager, false);

                    // Add the new reservation request as child to allocation
                    allocation.addChildReservationRequest(childReservationRequest);

                    // Create ACL entries for the new reservation request
                    authorizationManager.createAclEntriesForChildEntity(reservationRequestSet, childReservationRequest);

                    result.createdReservationRequests++;
                }

                // Update state for modified/new reservation request
                childReservationRequest.updateStateBySpecification();
            }

            // All child reservation requests that remains in list must be deleted
            List<Reservation> detachedReservations = new LinkedList<Reservation>();
            for (ReservationRequest reservationRequest : childReservationRequests) {
                // Remove child reservation request from allocation
                allocation.removeChildReservationRequest(reservationRequest);

                // Delete child reservation request and all it's ACL entries
                detachedReservations.addAll(
                        reservationRequestManager.hardDelete(reservationRequest, authorizationManager));

                result.deletedReservationRequests++;
            }
            for (Reservation reservation : detachedReservations) {
                allocation.addReservation(reservation);
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
            authorizationManager.commitTransaction(null);
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

    public static class Result
    {
        private int createdReservationRequests = 0;
        private int modifiedReservationRequests = 0;
        private int deletedReservationRequests = 0;

        public boolean isEmpty()
        {
            return createdReservationRequests == 0 &&
                    modifiedReservationRequests == 0 &&
                    deletedReservationRequests == 0;
        }

        public int getCreatedReservationRequests()
        {
            return createdReservationRequests;
        }

        public int getModifiedReservationRequests()
        {
            return modifiedReservationRequests;
        }

        public int getDeletedReservationRequests()
        {
            return deletedReservationRequests;
        }
    }
}
