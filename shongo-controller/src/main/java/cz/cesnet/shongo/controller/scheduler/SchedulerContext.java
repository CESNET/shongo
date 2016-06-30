package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.authorization.UserIdSet;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.recording.RecordingBucket;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.recording.RecordingServiceReservation;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.room.AvailableRoom;
import cz.cesnet.shongo.controller.booking.room.RoomBucket;
import cz.cesnet.shongo.controller.booking.room.RoomProviderCapability;
import cz.cesnet.shongo.controller.booking.room.RoomReservation;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.notification.AbstractNotification;
import cz.cesnet.shongo.util.RangeSet;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Context for the {@link ReservationTask}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SchedulerContext
{
    /**
     * {@link Cache} which can be used for allocating {@link Reservation}s.
     */
    private final Cache cache;

    /**
     * {@link EntityManager} which can be used for allocating {@link Reservation}s.
     */
    private final EntityManager entityManager;

    /**
     * {@link EntityManager} which can be used for allocating entities when something goes wrong.
     */
    private final EntityManager bypassEntityManager;

    /**
     * {@link AuthorizationManager} which can be used for allocating {@link Reservation}s.
     */
    private final AuthorizationManager authorizationManager;

    /**
     * Represents a minimum date/time before which the {@link Reservation}s cannot be allocated.
     */
    private final DateTime minimumDateTime;

    /**
     * Description for allocated {@link Reservation}s or {@link Executable}s.
     */
    private String description;

    /**
     * {@link ReservationRequestPurpose} for which the reservations are allocated.
     */
    private ReservationRequestPurpose purpose;

    /**
     * @see {@link AbstractReservationRequest#priority}
     */
    private int priority;

    /**
     * User-id of user who created reservation request.
     */
    private String userId;

    /**
     * @see SchedulerContextState
     */
    private SchedulerContextState state = new SchedulerContextState();

    /**
     * State to be updated for reservation request. Default {@code ALLOCATED}.
     */
    private ReservationRequest.AllocationState requestWantedState = ReservationRequest.AllocationState.ALLOCATED;

    /**
     * {@link ReservationRequest} by it's allocated {@link Reservation}.
     */
    private Map<Reservation, ReservationRequest> reservationRequestByReservation =
            new HashMap<Reservation, ReservationRequest>();

    /**
     * If perform of {@link ReservationTask} is just availability check.
     */
    private boolean availabilityCheck = false;

    /**
     * Constructor.
     *
     * @param minimumDateTime      sets the {@link #minimumDateTime}
     * @param cache                sets the {@link #cache}
     * @param entityManager        which can be used
     * @param authorizationManager which can be used
     * @param bypassEntityManager  which can be used when normal operation goes wrong
     */
    public SchedulerContext(DateTime minimumDateTime, Cache cache, EntityManager entityManager,
                            AuthorizationManager authorizationManager, EntityManager bypassEntityManager)
    {
        if (minimumDateTime == null) {
            throw new IllegalArgumentException("Minimum date/time must not be null.");
        }
        this.minimumDateTime = minimumDateTime;
        this.cache = cache;
        this.entityManager = entityManager;
        this.authorizationManager = authorizationManager;
        this.bypassEntityManager = bypassEntityManager;
    }

    /**
     * Constructor.
     */
    public SchedulerContext(DateTime minimumDateTime, Cache cache, EntityManager entityManager,
                            AuthorizationManager authorizationManager)
    {
        this(minimumDateTime, cache, entityManager, authorizationManager, null);
    }

    /**
     * @param purpose sets the {@link #purpose}
     */
    public void setPurpose(ReservationRequestPurpose purpose)
    {
        this.purpose = purpose;
    }

    /**
     * Initialize {@link SchedulerContext} from {@link ReservationRequest}.
     *
     * @param reservationRequest from which the {@link SchedulerContext} should be initialized
     */
    public void setReservationRequest(ReservationRequest reservationRequest)
    {
        this.description = reservationRequest.getDescription();
        this.purpose = reservationRequest.getPurpose();
        this.priority = reservationRequest.getPriority();

        userId = reservationRequest.getCreatedBy();
    }


    public String getUserId() {
        return userId;
    }

    public boolean isLocalByUser()
    {
        return UserInformation.isLocal(userId);
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * @param reusedAllocation which can be reused
     * @return reusable {@link Reservation}
     * @throws SchedulerException
     */
    public Reservation setReusableAllocation(Allocation reusedAllocation, Interval slot) throws SchedulerException
    {
        Reservation reusableReservation = getReusableReservation(reusedAllocation, slot);
        state.addAvailableReservation(reusableReservation, AvailableReservation.Type.REUSABLE);
        return reusableReservation;
    }

    /**
     * @return {@link #cache}
     */
    public Cache getCache()
    {
        return cache;
    }

    /**
     * @return {@link #entityManager}
     */
    public EntityManager getEntityManager()
    {
        return entityManager;
    }

    /**
     * @return {@link #bypassEntityManager}
     */
    public EntityManager getBypassEntityManager()
    {
        return bypassEntityManager;
    }

    /**
     * @return {@link #authorizationManager}
     */
    public AuthorizationManager getAuthorizationManager()
    {
        return authorizationManager;
    }

    /**
     * @return {@link #minimumDateTime}
     */
    public DateTime getMinimumDateTime()
    {
        return minimumDateTime;
    }

    /**
     * @return {@link #description}
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @return {@link #state}
     */
    public SchedulerContextState getState()
    {
        return state;
    }

    /**
     * @return {@link #requestWantedState}
     */
    public ReservationRequest.AllocationState getRequestWantedState()
    {
        return requestWantedState;
    }

    public void setRequestWantedState(ReservationRequest.AllocationState requestWantedState)
    {
        this.requestWantedState = requestWantedState;
    }

    /**
     * @return true whether executables should be allocated,
     * false otherwise
     */
    public boolean isExecutableAllowed()
    {
        return purpose == null || purpose.isExecutableAllowed();
    }

    /**
     * @return true whether only owned resource by the reservation request owner can be allocated,
     * false otherwise
     */
    public boolean isOwnerRestricted()
    {
        return purpose != null && purpose.isByOwner() || priority > 0;
    }

    /**
     * @return {@link #purpose} equals {@link ReservationRequestPurpose#MAINTENANCE}
     */
    public boolean isMaintenance()
    {
        return purpose.equals(ReservationRequestPurpose.MAINTENANCE);
    }

    /**
     * @return true whether maximum future and maximum duration should be checked,
     * false otherwise
     */
    public boolean isMaximumFutureAndDurationRestricted()
    {
        return purpose != null && !purpose.isByOwner();
    }

    public boolean isAvailabilityCheck()
    {
        return availabilityCheck;
    }

    public void setAvailabilityCheck(boolean availabilityCheck)
    {
        this.availabilityCheck = availabilityCheck;
    }

    /**
     * @param reservations
     * @return true whether all given {@code reservations} has lower priority than currently being allocated reservation,
     * false otherwise
     */
    public boolean hasHigherPriority(Collection<? extends Reservation> reservations)
    {
        for (Reservation reservation : reservations) {
            ReservationRequest reservationRequest = getReservationRequest(reservation);
            if (priority <= reservationRequest.getPriority()) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param resource whose owner should be checked
     * @return true if the {@link #userId} is an identifier of an owner
     * who is owner of given {@code resource}, false otherwise
     */
    public boolean containsCreatedByUserId(Resource resource)
    {
        UserIdSet resourceOwnerIds = authorizationManager.getUserIdsWithRole(resource, ObjectRole.OWNER);
        if (resourceOwnerIds.isEveryone()) {
            return true;
        }
        if (resourceOwnerIds.size() == 0) {
            resourceOwnerIds.add(resource.getUserId());
        }
        return resourceOwnerIds.contains(userId);
    }

    /**
     * @param roomProviderCapability
     * @param slot
     * @param reservationTask
     * @return {@link AvailableRoom} for given {@code roomProviderCapability} in given {@code interval}
     */
    public AvailableRoom getAvailableRoom(RoomProviderCapability roomProviderCapability, Interval slot,
            ReservationTask reservationTask)
    {
        int usedLicenseCount = 0;
        ResourceCache resourceCache = cache.getResourceCache();
        if (resourceCache.isResourceAvailable(roomProviderCapability.getResource(), slot, this, reservationTask)) {
            ReservationManager reservationManager = new ReservationManager(entityManager);
            List<RoomReservation> roomReservations =
                    reservationManager.getRoomReservations(roomProviderCapability, slot);

            usedLicenseCount = getLicenseCountPeak(slot, roomReservations, roomProviderCapability);
        }
        else {
            usedLicenseCount = roomProviderCapability.getLicenseCount();
        }
        return new AvailableRoom(roomProviderCapability, usedLicenseCount);
    }

    /**
     * @param slot
     * @param roomReservations
     * @param roomProviderCapability
     * @return peak of licenses count used by {@code roomReservations}
     */
    public int getLicenseCountPeak(Interval slot, List<RoomReservation> roomReservations, RoomProviderCapability roomProviderCapability)
    {
        state.applyReservations(roomProviderCapability.getId(), slot, roomReservations, RoomReservation.class);
        RangeSet<RoomReservation, DateTime> rangeSet = new RangeSet<RoomReservation, DateTime>()
        {
            @Override
            protected Bucket<DateTime, RoomReservation> createBucket(DateTime rangeValue)
            {
                return new RoomBucket(rangeValue);
            }
        };
        for (RoomReservation roomReservation : roomReservations) {
            rangeSet.add(roomReservation, roomReservation.getSlotStart(), roomReservation.getSlotEnd());
        }

        List<RoomBucket> roomBuckets = new LinkedList<>();
        roomBuckets.addAll(rangeSet.getBuckets(slot.getStart(), slot.getEnd(), RoomBucket.class));
        Collections.sort(roomBuckets, new Comparator<RoomBucket>()
        {
            @Override
            public int compare(RoomBucket roomBucket1, RoomBucket roomBucket2)
            {
                return -Double.compare(roomBucket1.getLicenseCount(), roomBucket2.getLicenseCount());
            }
        });
        if (roomBuckets.size() > 0) {
            RoomBucket roomBucket = roomBuckets.get(0);
            return roomBucket.getLicenseCount();
        }
        return 0;
    }


    /**
     * @param slot
     * @param recordingReservations
     * @param recordingCapability
     * @return peak of licenses count used by {@code recordingReservations}
     */
    public int getLicenseCountPeak(Interval slot, List<RecordingServiceReservation> recordingReservations, RecordingCapability recordingCapability)
    {
        state.applyReservations(recordingCapability.getId(), slot, recordingReservations, RecordingServiceReservation.class);
        RangeSet<RecordingServiceReservation, DateTime> rangeSet = new RangeSet<RecordingServiceReservation, DateTime>()
        {
            @Override
            protected Bucket<DateTime, RecordingServiceReservation> createBucket(DateTime rangeValue)
            {
                return new RecordingBucket(rangeValue);
            }
        };
        for (RecordingServiceReservation recordingReservation : recordingReservations) {
            rangeSet.add(recordingReservation, recordingReservation.getSlotStart(), recordingReservation.getSlotEnd());
        }

        List<RecordingBucket> recordingBuckets = new LinkedList<>();
        recordingBuckets.addAll(rangeSet.getBuckets(slot.getStart(), slot.getEnd(), RecordingBucket.class));
        Collections.sort(recordingBuckets, new Comparator<RecordingBucket>()
        {
            @Override
            public int compare(RecordingBucket recordingBucket1, RecordingBucket recordingBucket2)
            {
                return -Double.compare(recordingBucket1.getLicenseCount(), recordingBucket2.getLicenseCount());
            }
        });
        if (recordingBuckets.size() > 0) {
            RecordingBucket recordingBucket = recordingBuckets.get(0);
            return recordingBucket.getLicenseCount();
        }
        return 0;
    }

    /**
     * @param allocation
     * @param slot
     * @return {@link Reservation} which can be reused from given {@code allocation} for {@code slot}
     * @throws SchedulerException
     */
    public Reservation getReusableReservation(Allocation allocation, Interval slot)
            throws SchedulerException
    {
        AbstractReservationRequest reservationRequest = allocation.getReservationRequest();

        // Find reusable reservation
        Reservation reusableReservation = null;
        Interval reservationInterval = null;
        for (Reservation reservation : allocation.getReservations()) {
            reservationInterval = reservation.getSlot();
            if (reservationInterval.contains(slot)) {
                reusableReservation = reservation;
                break;
            }
        }
        if (reusableReservation == null) {
            throw new SchedulerReportSet.ReservationRequestInvalidSlotException(reservationRequest,
                    reservationInterval);
        }

        // Check the reusable reservation
        ReservationManager reservationManager = new ReservationManager(entityManager);
        List<ExistingReservation> existingReservations =
                reservationManager.getExistingReservations(reusableReservation, slot);
        state.applyAvailableReservations(existingReservations, ExistingReservation.class);
        if (existingReservations.size() > 0) {
            ExistingReservation existingReservation = existingReservations.get(0);
            Interval usageSlot = existingReservation.getSlot();
            Reservation usageReservation = existingReservation.getTopReservation();
            AbstractReservationRequest usageReservationRequest = usageReservation.getReservationRequest();
            throw new SchedulerReportSet.ReservationAlreadyUsedException(reusableReservation, reservationRequest,
                    usageReservationRequest, usageSlot);
        }
        return reusableReservation;
    }

    /**
     * Delete all {@link SchedulerContextState#reservationsToDelete}.
     *
     * @return list of {@link AbstractNotification}s
     */
    public List<AbstractNotification> finish(Scheduler.Result result)
    {
        ReservationManager reservationManager = new ReservationManager(entityManager);
        for (Reservation reservation : state.getReservationsToDelete()) {
            reservationManager.delete(reservation, minimumDateTime, authorizationManager);
            result.deletedReservations++;
        }
        return state.getNotifications();
    }

    /**
     * @param reservation
     * @return {@link ReservationRequest} for which is allocated given {@code reservation}
     */
    private ReservationRequest getReservationRequest(Reservation reservation)
    {
        ReservationRequest reservationRequest = reservationRequestByReservation.get(reservation);
        if (reservationRequest == null) {
            Reservation topReservation = reservation.getTopReservation();
            Allocation allocation = topReservation.getAllocation();
            if (allocation == null) {
                throw new TodoImplementException("Reservation doesn't have allocation.");
            }
            reservationRequest = (ReservationRequest) allocation.getReservationRequest();
            if (reservationRequest == null) {
                throw new TodoImplementException("Reservation allocation doesn't have reservation request.");
            }
            reservationRequestByReservation.put(reservation, reservationRequest);
        }
        return reservationRequest;
    }

    /**
     * @param reservationTask
     * @param collidingReservations
     * @return true whether a collision exists and the allocation should be aborted,
     *         false otherwise
     */
    public boolean detectCollisions(ReservationTask reservationTask, List<? extends Reservation> collidingReservations)
            throws SchedulerException
    {
        if (collidingReservations.size() == 0) {
            // No colliding reservation exists
            return false;
        }

        if (hasHigherPriority(collidingReservations)) {
            // Reallocate all colliding reservations
            List<String> collection = new LinkedList<String>();
            for (Reservation reservation : collidingReservations) {
                ReservationRequest reservationRequest = getReservationRequest(reservation);
                // Reallocate colliding reservation request
                state.forceReservationRequestReallocation(reservationRequest);
                // Add reservation request to collection
                collection.add(ObjectIdentifier.formatId(reservation));
            }
            reservationTask.addReport(new SchedulerReportSet.ReallocatingReservationRequestsReport(collection));
            return false;
        }
        if (isMaintenance()) {
            Map<String, String> map = new LinkedHashMap<String, String>();
            for (Reservation reservation : collidingReservations) {
                ReservationRequest reservationRequest = getReservationRequest(reservation);
                // Reallocate colliding reservation request
                state.tryReservationRequestReallocation(reservationRequest);
                // Add reservation request by reservation to map
                map.put(ObjectIdentifier.formatId(reservation), ObjectIdentifier.formatId(reservationRequest));
            }
            reservationTask.addReport(new SchedulerReportSet.CollidingReservationsReport(map));
            return false;
        }
        Reservation reservation = collidingReservations.get(0);

        if (ReservationRequestPurpose.MAINTENANCE.equals(reservation.getAllocation().getReservationRequest().getPurpose())) {
            throw new SchedulerReportSet.ResourceUnderMaintenanceException(reservation.getAllocatedResource(), reservation.getSlot());
        }
        throw new SchedulerReportSet.ResourceAlreadyAllocatedException(reservation.getAllocatedResource(), reservation.getSlot());
    }

}
