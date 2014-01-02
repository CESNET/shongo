package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability;
import cz.cesnet.shongo.controller.booking.alias.AliasReservation;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.reservation.TargetedReservation;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.resource.ResourceReservation;
import cz.cesnet.shongo.controller.booking.room.AvailableRoom;
import cz.cesnet.shongo.controller.booking.room.RoomProviderCapability;
import cz.cesnet.shongo.controller.booking.room.RoomReservation;
import cz.cesnet.shongo.controller.booking.value.ValueReservation;
import cz.cesnet.shongo.controller.booking.value.provider.ValueProvider;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.util.RangeSet;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Context for the {@link ReservationTask}.
 */
public class SchedulerContext
{
    /**
     * @see cz.cesnet.shongo.controller.cache.Cache
     */
    private final Cache cache;

    /**
     * Entity manager.
     */
    private final EntityManager entityManager;

    /**
     * @see cz.cesnet.shongo.controller.authorization.AuthorizationManager
     */
    private final AuthorizationManager authorizationManager;

    /**
     * Represents a minimum date/time before which the {@link cz.cesnet.shongo.controller.booking.reservation.Reservation}s cannot be allocated.
     */
    private final DateTime minimumDateTime;

    /**
     * Requested slot for which the {@link cz.cesnet.shongo.controller.booking.reservation.Reservation}s should be allocated.
     */
    private Interval requestedSlot;

    /**
     * Description for allocated reservations or executables.
     */
    private String description;

    /**
     * {@link ReservationRequestPurpose} for which the reservations are allocated.
     */
    private ReservationRequestPurpose purpose;

    /**
     * Set of user-ids for which the reservations are being allocated.
     */
    private Set<String> userIds = new HashSet<String>();

    /**
     * Set of {@link AvailableReservation}s.
     */
    private Set<AvailableReservation<? extends Reservation>> availableReservations =
            new HashSet<AvailableReservation<? extends Reservation>>();

    /**
     * Map of {@link AvailableReservation}s by {@link AvailableReservation#originalReservation}s.
     */
    private Map<Reservation, AvailableReservation<? extends Reservation>> availableReservationByOriginalReservation =
            new HashMap<Reservation, AvailableReservation<? extends Reservation>>();

    /**
     * Map of {@link AvailableExecutable}s by {@link Executable}s.
     */
    private Map<Executable, AvailableExecutable> availableExecutables = new HashMap<Executable, AvailableExecutable>();

    /**
     * Set of allocated {@link Reservation}s.
     */
    private Set<Reservation> allocatedReservations = new HashSet<Reservation>();

    /**
     * {@link ReservationTransaction} by {@link Reservation} class.
     */
    private Map<Class<? extends TargetedReservation>, ReservationTransaction<TargetedReservation>> reservationTransactionByType =
            new HashMap<Class<? extends TargetedReservation>, ReservationTransaction<TargetedReservation>>();

    /**
     * Set of resources referenced from {@link ResourceReservation}s in the context.
     */
    private Set<Resource> referencedResources = new HashSet<Resource>();

    /**
     * List of {@link ReservationRequest} which should be reallocated.
     */
    private List<ReservationRequest> reservationRequestsToReallocate = new LinkedList<ReservationRequest>();

    /**
     * List of {@link Reservation}s which should be deleted.
     */
    private List<Reservation> reservationsToDelete = new LinkedList<Reservation>();

    /**
     * Current {@link Savepoint} to which are recorded all performed changes.
     */
    private Savepoint currentSavepoint;

    /**
     * Constructor.
     *
     * @param minimumDateTime      sets the {@link #minimumDateTime}
     * @param cache                sets the {@link #cache}
     * @param entityManager        which can be used
     * @param authorizationManager which can be used
     */
    public SchedulerContext(DateTime minimumDateTime, Cache cache, EntityManager entityManager,
            AuthorizationManager authorizationManager)
    {
        if (minimumDateTime == null) {
            throw new IllegalArgumentException("Minimum date/time must not be null.");
        }
        this.minimumDateTime = minimumDateTime;
        this.cache = cache;
        this.entityManager = entityManager;
        this.authorizationManager = authorizationManager;
    }

    /**
     * Constructor.
     *
     * @param cache         sets the {@link #cache}
     * @param entityManager which can be used or null
     * @param authorization which can be used or null
     * @param requestedSlot sets the {@link #requestedSlot}
     */
    public SchedulerContext(Cache cache, EntityManager entityManager, Authorization authorization,
            Interval requestedSlot)
    {
        this(requestedSlot.getStart(), cache, entityManager, new AuthorizationManager(entityManager, authorization));
        setRequestedSlot(requestedSlot);
    }

    /**
     * @param requestedSlot sets the {@link #requestedSlot}
     */
    public void setRequestedSlot(Interval requestedSlot)
    {
        if (requestedSlot.isBefore(minimumDateTime)) {
            throw new IllegalArgumentException("Requested slot can't entirely belong to history.");
        }
        this.requestedSlot = requestedSlot;

        // Update requested slot to not allocate before minimum date/time
        if (this.requestedSlot.contains(minimumDateTime)) {
            this.requestedSlot = new Interval(minimumDateTime, this.requestedSlot.getEnd());
        }
    }

    /**
     * @param requestSlotStart sets the start of the {@link #requestedSlot}
     */
    public void setRequestedSlotStart(DateTime requestSlotStart)
    {
        if (requestedSlot == null) {
            throw new IllegalStateException("Requested slot hasn't been set yet.");
        }
        if (requestSlotStart.isBefore(minimumDateTime)) {
            throw new IllegalArgumentException("Requested slot can't start before minimum date/time.");
        }
        requestedSlot = new Interval(requestSlotStart, requestedSlot.getEnd());
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
        setRequestedSlot(reservationRequest.getSlot());

        this.description = reservationRequest.getDescription();
        this.purpose = reservationRequest.getPurpose();

        userIds.clear();
        userIds.addAll(authorizationManager.getUserIdsWithRole(reservationRequest, ObjectRole.OWNER));
        if (userIds.size() == 0) {
            userIds.add(reservationRequest.getCreatedBy());
        }
    }

    /**
     * @param reusedAllocation which can be reused
     * @return reusable {@link Reservation}
     * @throws SchedulerException
     */
    public Reservation setReusableAllocation(Allocation reusedAllocation) throws SchedulerException
    {
        Reservation reusableReservation = getReusableReservation(reusedAllocation);
        addAvailableReservation(reusableReservation, AvailableReservation.Type.REUSABLE);
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
     * @return {@link #requestedSlot}
     */
    public Interval getRequestedSlot()
    {
        return requestedSlot;
    }

    /**
     * @return {@link #requestedSlot} start
     */
    public DateTime getRequestedSlotStart()
    {
        return requestedSlot.getStart();
    }

    /**
     * @return {@link #description}
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @return true whether executables should be allocated,
     *         false otherwise
     */
    public boolean isExecutableAllowed()
    {
        return purpose == null || purpose.isExecutableAllowed();
    }

    /**
     * @return true whether only owned resource by the reservation request owner can be allocated,
     *         false otherwise
     */
    public boolean isOwnerRestricted()
    {
        return purpose != null && purpose.isByOwner();
    }

    /**
     * @return true whether maximum future and maximum duration should be checked,
     *         false otherwise
     */
    public boolean isMaximumFutureAndDurationRestricted()
    {
        return purpose != null && !purpose.isByOwner();
    }

    /**
     * @param resource whose owner should be checked
     * @return true if the {@link #userIds} contains an identifier of an owner
     *         who is owner of given {@code resource}, false otherwise
     */
    public boolean containsOwnerUserId(Resource resource)
    {
        Set<String> resourceOwnerIds = new HashSet<String>();
        resourceOwnerIds.addAll(authorizationManager.getUserIdsWithRole(resource, ObjectRole.OWNER));
        if (resourceOwnerIds.size() == 0) {
            resourceOwnerIds.add(resource.getUserId());
        }
        return !Collections.disjoint(resourceOwnerIds, userIds);
    }

    /**
     * @return {@link #currentSavepoint}
     */
    public Savepoint getCurrentSavepoint()
    {
        return currentSavepoint;
    }

    /**
     * @return {@link #referencedResources}
     */
    public Set<Resource> getReferencedResources()
    {
        return Collections.unmodifiableSet(referencedResources);
    }

    /**
     * @param resource to be checked
     * @return true if given resource was referenced by any {@link ResourceReservation} added to the context,
     *         false otherwise
     */
    public boolean containsReferencedResource(Resource resource)
    {
        return referencedResources.contains(resource);
    }

    /**
     * @return {@link #allocatedReservations}
     */
    public Set<Reservation> getAllocatedReservations()
    {
        return Collections.unmodifiableSet(allocatedReservations);
    }

    /**
     * @return {@link #availableReservations}
     */
    public Set<AvailableReservation<? extends Reservation>> getAvailableReservations()
    {
        return Collections.unmodifiableSet(availableReservations);
    }

    /**
     * Remove {@link #availableReservations} from given {@code reservations}.
     *
     * @param reservations
     */
    public <T extends Reservation> void applyAvailableReservations(Collection<T> reservations, Class<T> reservationType)
    {
        for (AvailableReservation<? extends Reservation> availableReservation : availableReservations) {
            Reservation reservation = availableReservation.getOriginalReservation();
            if (reservationType.isInstance(reservation)) {
                reservations.remove(reservationType.cast(reservation));
            }
        }
    }

    /**
     * @param executableType
     * @return collection of available {@link Executable}s of given {@code executableType}
     */
    public <E extends Executable> Collection<AvailableExecutable<E>> getAvailableExecutables(Class<E> executableType)
    {
        Set<AvailableExecutable<E>> availableExecutables = new HashSet<AvailableExecutable<E>>();
        for (AvailableExecutable availableExecutable : this.availableExecutables.values()) {
            if (executableType.isInstance(availableExecutable.getExecutable())) {
                @SuppressWarnings("unchecked")
                AvailableExecutable<E> typedAvailableExecutable = availableExecutable;
                availableExecutables.add(typedAvailableExecutable);
            }
        }
        return availableExecutables;
    }

    /**
     * @param objectId for which the {@link AvailableReservation}s should be returned
     * @return {@link AvailableReservation}s ({@link ResourceReservation}s) for given {@code resource}
     */
    public <T extends TargetedReservation> Set<AvailableReservation<T>> getAvailableReservations(Long objectId,
            Class<T> reservationType)
    {
        @SuppressWarnings("unchecked")
        ReservationTransaction<T> reservationTransaction = (ReservationTransaction<T>)
                reservationTransactionByType.get(getReservationTransactionType(reservationType));
        if (reservationTransaction == null) {
            return Collections.emptySet();
        }
        return reservationTransaction.getAvailableReservations(objectId);
    }

    /**
     * @param aliasProvider
     * @return collection of {@link AvailableReservation}s ({@link AliasReservation}s) for given {@code aliasProvider}
     */
    public Collection<AvailableReservation<AliasReservation>> getAvailableAliasReservations(
            AliasProviderCapability aliasProvider)
    {
        return getAvailableReservations(aliasProvider.getId(), AliasReservation.class);
    }

    /**
     * @param resource for which the {@link AvailableReservation}s should be returned
     * @return {@link AvailableReservation}s ({@link ResourceReservation}s) for given {@code resource}
     */
    public Set<AvailableReservation<ResourceReservation>> getAvailableResourceReservations(Resource resource)
    {
        return getAvailableReservations(resource.getId(), ResourceReservation.class);
    }

    /**
     * @param valueProvider for which the {@link AvailableReservation}s should be returned
     * @return {@link AvailableReservation}s ({@link ValueReservation}s) for given {@code valueProvider}
     */
    public Set<AvailableReservation<ValueReservation>> getAvailableValueReservations(ValueProvider valueProvider)
    {
        return getAvailableReservations(valueProvider.getId(), ValueReservation.class);
    }

    /**
     * @param roomProvider for which the {@link AvailableReservation}s should be returned
     * @return {@link AvailableReservation}s ({@link RoomReservation}s) for given {@code roomProvider}
     */
    public Set<AvailableReservation<RoomReservation>> getAvailableRoomReservations(RoomProviderCapability roomProvider)
    {
        return getAvailableReservations(roomProvider.getId(), RoomReservation.class);
    }

    /**
     * @param reservation to be added to the {@link SchedulerContext} as already allocated.
     */
    public void addAllocatedReservation(Reservation reservation)
    {
        if (!allocatedReservations.add(reservation)) {
            // Reservation is already added as allocated to the context
            return;
        }
        onChange(ObjectType.ALLOCATED_RESERVATION, reservation, ObjectState.ADDED);

        if (reservation.getSlot().contains(getRequestedSlot()) && reservation instanceof TargetedReservation) {
            TargetedReservation targetedReservation = (TargetedReservation) reservation;
            Class<? extends TargetedReservation> reservationType = getReservationTransactionType(targetedReservation);
            ReservationTransaction<TargetedReservation> reservationTransaction =
                    reservationTransactionByType.get(reservationType);
            if (reservationTransaction == null) {
                reservationTransaction = new ReservationTransaction<TargetedReservation>();
                reservationTransactionByType.put(reservationType, reservationTransaction);
            }
            reservationTransaction.addAllocatedReservation(
                    targetedReservation.getTargetId(), targetedReservation);
        }
    }

    /**
     * @param reservation to be removed from the {@link SchedulerContext} as already allocated.
     */
    public void removeAllocatedReservation(Reservation reservation)
    {
        if (!allocatedReservations.remove(reservation)) {
            // Reservation is not added as allocated to the context
            return;
        }
        onChange(ObjectType.ALLOCATED_RESERVATION, reservation, ObjectState.REMOVED);

        if (reservation.getSlot().contains(getRequestedSlot()) && reservation instanceof TargetedReservation) {

            TargetedReservation targetedReservation = (TargetedReservation) reservation;
            Class<? extends TargetedReservation> reservationType = getReservationTransactionType(targetedReservation);
            ReservationTransaction<TargetedReservation> reservationTransaction =
                    reservationTransactionByType.get(reservationType);
            if (reservationTransaction != null) {
                reservationTransaction.removeAllocatedReservation(
                        targetedReservation.getTargetId(), targetedReservation);
            }
        }
    }

    /**
     * @param resource to be added to the {@link #referencedResources}
     */
    public void addReferencedResource(Resource resource)
    {
        if (referencedResources.add(resource)) {
            onChange(ObjectType.REFERENCED_RESOURCE, resource, ObjectState.ADDED);
        }
    }

    /**
     * @param resource to be removed from the {@link #referencedResources}
     */
    public void removeReferencedResource(Resource resource)
    {
        if (referencedResources.remove(resource)) {
            onChange(ObjectType.REFERENCED_RESOURCE, resource, ObjectState.REMOVED);
        }
    }

    /**
     * @param originalReservation to be added to the {@link SchedulerContext} as available
     * @param type                of available reservation
     */
    public AvailableReservation<? extends Reservation> addAvailableReservation(Reservation originalReservation,
            AvailableReservation.Type type)
    {
        if (availableReservationByOriginalReservation.containsKey(originalReservation)) {
            // Reservation is already added as available to the context
            AvailableReservation<? extends Reservation> availableReservation =
                    availableReservationByOriginalReservation.get(originalReservation);
            if (!availableReservation.isType(type)) {
                throw new IllegalArgumentException("Reservation is already added with different type.");
            }
            return availableReservation;
        }
        AvailableReservation<? extends Reservation> availableReservation =
                AvailableReservation.create(originalReservation, type);
        addAvailableReservation(availableReservation);
        return availableReservation;
    }

    /**
     * @param availableReservation to be added to the {@link SchedulerContext}
     */
    public void addAvailableReservation(AvailableReservation<? extends Reservation> availableReservation)
    {
        if (!availableReservations.add(availableReservation)) {
            // Reservation is already added as available to the context
            return;
        }
        Reservation originalReservation = availableReservation.getOriginalReservation();
        Reservation targetReservation = availableReservation.getTargetReservation();
        AvailableReservation.Type type = availableReservation.getType();
        availableReservationByOriginalReservation.put(originalReservation, availableReservation);
        onChange(ObjectType.AVAILABLE_RESERVATION, availableReservation, ObjectState.ADDED);

        if (availableReservation.isType(AvailableReservation.Type.REUSABLE)) {
            Executable executable = targetReservation.getExecutable();
            if (executable != null) {
                if (!availableExecutables.containsKey(executable)) {
                    availableExecutables.put(executable,
                            new AvailableExecutable<Executable>(executable, availableReservation));
                }
            }
        }

        if (targetReservation instanceof TargetedReservation) {
            TargetedReservation targetedReservation = (TargetedReservation) targetReservation;
            Class<? extends TargetedReservation> reservationType = getReservationTransactionType(targetedReservation);
            ReservationTransaction<TargetedReservation> reservationTransaction =
                    reservationTransactionByType.get(reservationType);
            if (reservationTransaction == null) {
                reservationTransaction = new ReservationTransaction<TargetedReservation>();
                reservationTransactionByType.put(reservationType, reservationTransaction);
            }
            reservationTransaction.addAvailableReservation(
                    targetedReservation.getTargetId(), availableReservation.cast(TargetedReservation.class));
        }

        // Add all child reservations
        for (Reservation childReservation : originalReservation.getChildReservations()) {
            addAvailableReservation(childReservation, type);
        }
    }

    /**
     * @param availableReservation to be removed from the {@link SchedulerContext}
     */
    public void removeAvailableReservation(AvailableReservation<? extends Reservation> availableReservation)
    {
        removeAvailableReservation(availableReservation, true, true);
    }

    /**
     * @param availableReservation to be removed from the {@link SchedulerContext}
     * @param removeParent         specifies whether parent {@code availableReservation} should be removed
     * @param removeChildren       specifies whether child {@code availableReservation} should be removed
     */
    private void removeAvailableReservation(AvailableReservation<? extends Reservation> availableReservation,
            boolean removeParent, boolean removeChildren)
    {
        if (!availableReservations.remove(availableReservation)) {
            // Reservation is not added to the context
            return;
        }
        onChange(ObjectType.AVAILABLE_RESERVATION, availableReservation, ObjectState.REMOVED);

        Reservation targetReservation = availableReservation.getTargetReservation();
        Reservation originalReservation = availableReservation.getOriginalReservation();
        availableReservationByOriginalReservation.remove(originalReservation);

        Executable executable = targetReservation.getExecutable();
        if (executable != null) {
            availableExecutables.remove(executable);
        }

        if (targetReservation instanceof TargetedReservation) {
            TargetedReservation targetedReservation = (TargetedReservation) targetReservation;
            Class<? extends TargetedReservation> reservationType = getReservationTransactionType(targetedReservation);
            ReservationTransaction<TargetedReservation> reservationTransaction =
                    reservationTransactionByType.get(reservationType);
            if (reservationTransaction != null) {
                reservationTransaction.removeAvailableReservation(
                        targetedReservation.getTargetId(), availableReservation.cast(TargetedReservation.class));
            }
        }

        // Remove all reservations (recursive)
        if (removeParent) {
            Reservation parentReservation = originalReservation.getParentReservation();
            if (parentReservation != null) {
                if (availableReservation.isModifiable()) {
                    originalReservation.setParentReservation(null);
                }
                else {
                    AvailableReservation<? extends Reservation> parentAvailableReservation =
                            availableReservationByOriginalReservation.get(parentReservation);
                    if (parentAvailableReservation != null) {
                        removeAvailableReservation(parentAvailableReservation, true, false);
                    }
                }
            }
        }

        // Remove child reservations (recursive)
        if (removeChildren) {
            List<Reservation> childReservations = new LinkedList<Reservation>(
                    originalReservation.getChildReservations());
            for (Reservation childReservation : childReservations) {
                AvailableReservation<? extends Reservation> childAvailableReservation =
                        availableReservationByOriginalReservation.get(childReservation);
                if (childAvailableReservation == null) {
                    throw new RuntimeException("Child reservation should be available.");
                }
                removeAvailableReservation(childAvailableReservation, false, true);
            }
        }
    }

    /**
     * Record transaction change.
     *
     * @param objectType
     * @param object
     * @param objectState
     */
    private void onChange(ObjectType objectType, Object object, ObjectState objectState)
    {
        if (currentSavepoint != null) {
            if (currentSavepoint.nextSavepoint != null) {
                throw new RuntimeException("Current savepoint shouldn't have next savepoint.");
            }
            Map<Object, ObjectState> objectTypeChanges = currentSavepoint.changes.get(objectType);
            if (objectTypeChanges == null) {
                objectTypeChanges = new HashMap<Object, ObjectState>();
                currentSavepoint.changes.put(objectType, objectTypeChanges);
            }
            ObjectState oldObjectState = objectTypeChanges.get(object);
            if (oldObjectState != null) {
                if (oldObjectState != objectState) {
                    objectTypeChanges.remove(object);
                }
                else {
                    throw new RuntimeException("Cannot do the same change.");
                }
            }
            else {
                objectTypeChanges.put(object, objectState);
            }
        }
    }

    /**
     * @return new {@link Savepoint} for this {@link SchedulerContext}
     */
    public Savepoint createSavepoint()
    {
        Savepoint savepoint = new Savepoint();
        savepoint.previousSavepoint = currentSavepoint;
        currentSavepoint = savepoint;
        return savepoint;
    }

    /**
     * Apply {@link #reservationTransactionByType} to given {@code reservations}.
     *
     * @param resourceId
     * @param reservations
     */
    public <T extends TargetedReservation> void applyReservations(Long resourceId, List<T> reservations,
            Class<T> reservationType)
    {
        @SuppressWarnings("unchecked")
        ReservationTransaction<T> reservationTransaction = (ReservationTransaction<T>)
                reservationTransactionByType.get(getReservationTransactionType(reservationType));
        if (reservationTransaction != null) {
            reservationTransaction.applyReservations(resourceId, reservations);
        }
    }

    /**
     * @param roomProviderCapability
     * @return {@link cz.cesnet.shongo.controller.booking.room.AvailableRoom} for given {@code roomProviderCapability} in given {@code interval}
     */
    public AvailableRoom getAvailableRoom(RoomProviderCapability roomProviderCapability)
    {
        int usedLicenseCount = 0;
        if (cache.getResourceCache().isResourceAvailable(roomProviderCapability.getResource(), this)) {
            ReservationManager reservationManager = new ReservationManager(entityManager);
            List<RoomReservation> roomReservations =
                    reservationManager.getRoomReservations(roomProviderCapability, requestedSlot);
            applyReservations(roomProviderCapability.getId(), roomReservations, RoomReservation.class);
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

            List<RoomBucket> roomBuckets = new LinkedList<RoomBucket>();
            roomBuckets.addAll(rangeSet.getBuckets(requestedSlot.getStart(), requestedSlot.getEnd(), RoomBucket.class));
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
                usedLicenseCount = roomBucket.getLicenseCount();
            }
        }
        else {
            usedLicenseCount = roomProviderCapability.getLicenseCount();
        }
        return new AvailableRoom(roomProviderCapability, usedLicenseCount);
    }

    /**
     * @param reservation to be deleted
     */
    public void addReservationToDelete(Reservation reservation)
    {
        reservationsToDelete.add(reservation);
    }

    /**
     * @return iterator of {@link #reservationRequestsToReallocate}
     */
    public Iterator<ReservationRequest> getReservationRequestsToReallocate()
    {
        return reservationRequestsToReallocate.iterator();
    }

    /**
     * @param reservationRequest to be reallocated
     */
    public void addReservationRequestToReallocate(ReservationRequest reservationRequest)
    {
        reservationRequestsToReallocate.add(reservationRequest);
    }

    /**
     * Delete all {@link #reservationsToDelete}.
     */
    public void finish()
    {
        ReservationManager reservationManager = new ReservationManager(entityManager);
        for (Reservation reservation : reservationsToDelete) {
            reservationManager.delete(reservation, authorizationManager);
        }
    }

    /**
     * @param targetedReservation
     * @return type for {@link #reservationTransactionByType}
     */
    private Class<? extends TargetedReservation> getReservationTransactionType(TargetedReservation targetedReservation)
    {
        return getReservationTransactionType(targetedReservation.getClass());
    }

    /**
     * @param reservationType
     * @return type for {@link #reservationTransactionByType}
     */
    private Class<? extends TargetedReservation> getReservationTransactionType(
            Class<? extends TargetedReservation> reservationType)
    {
        if (ResourceReservation.class.isAssignableFrom(reservationType)) {
            return ResourceReservation.class;
        }
        else if (ValueReservation.class.isAssignableFrom(reservationType)) {
            return ValueReservation.class;
        }
        return reservationType;
    }

    /**
     * Represents a savepoint for the {@link SchedulerContext} to which it can be reverted.
     */
    public class Savepoint
    {
        /**
         * Previous {@link Savepoint}.
         */
        private Savepoint previousSavepoint;

        /**
         * Next {@link Savepoint}.
         */
        private Savepoint nextSavepoint;

        /**
         * Changes which were made after the {@link Savepoint} and which are supposed to be reverted.
         */
        private Map<ObjectType, Map<Object, ObjectState>> changes =
                new HashMap<ObjectType, Map<Object, ObjectState>>();

        /**
         * Revert changes performed by this and all next {@link Savepoint}s.
         */
        public void revert()
        {
            // Revert next savepoint first (recursively)
            if (nextSavepoint != null) {
                nextSavepoint.revert();
            }

            // Disable logging of the reverting
            Savepoint storedCurrentSavepoint = currentSavepoint;
            currentSavepoint = null;

            // Revert changes
            for (ObjectType objectType : changes.keySet()) {
                Map<Object, ObjectState> objectTypeChanges = changes.get(objectType);
                switch (objectType) {
                    case REFERENCED_RESOURCE:
                        for (Object object : objectTypeChanges.keySet()) {
                            ObjectState objectState = objectTypeChanges.get(object);
                            if (objectState == ObjectState.ADDED) {
                                removeReferencedResource((Resource) object);
                            }
                            else {
                                addReferencedResource((Resource) object);
                            }
                        }
                        break;
                    case ALLOCATED_RESERVATION:
                        for (Object object : objectTypeChanges.keySet()) {
                            ObjectState objectState = objectTypeChanges.get(object);
                            if (objectState == ObjectState.ADDED) {
                                removeAllocatedReservation((Reservation) object);
                            }
                            else {
                                addAllocatedReservation((Reservation) object);
                            }
                        }
                        break;
                    case AVAILABLE_RESERVATION:
                        for (Object object : objectTypeChanges.keySet()) {
                            @SuppressWarnings("unchecked")
                            AvailableReservation<Reservation> availableReservation =
                                    (AvailableReservation<Reservation>) object;
                            ObjectState objectState = objectTypeChanges.get(object);
                            if (objectState == ObjectState.ADDED) {
                                removeAvailableReservation(availableReservation, false, false);
                            }
                            else {
                                addAvailableReservation(availableReservation);
                            }
                        }
                        break;
                    default:
                        throw new TodoImplementException(objectType);
                }
            }

            // Restore current savepoint
            currentSavepoint = storedCurrentSavepoint;

            destroy();
        }

        /**
         * Destroy this and all next {@link Savepoint}s (changes are not reverted).
         */
        public void destroy()
        {
            if (changes == null) {
                // Savepoint has been already destroyed
                return;
            }

            // Destroy next savepoint first (recursively)
            if (nextSavepoint != null) {
                nextSavepoint.destroy();
            }

            // If this is current savepoint
            if (currentSavepoint == this) {
                // Move current savepoint to previous the previous one
                currentSavepoint = previousSavepoint;
            }

            // Previous savepoint should not more know about this
            if (previousSavepoint != null) {
                previousSavepoint.nextSavepoint = null;
            }

            // Destroy changes and previous savepoint
            changes = null;
            previousSavepoint = null;
        }
    }

    /**
     * @param allocation
     * @return {@link Reservation} which can be reused from given {@code allocation} for {@link #requestedSlot}
     * @throws SchedulerException
     */
    public Reservation getReusableReservation(Allocation allocation)
            throws SchedulerException
    {
        AbstractReservationRequest reservationRequest = allocation.getReservationRequest();

        // Find reusable reservation
        Reservation reusableReservation = null;
        Interval reservationInterval = null;
        for (Reservation reservation : allocation.getReservations()) {
            reservationInterval = reservation.getSlot();
            if (reservationInterval.contains(requestedSlot)) {
                reusableReservation = reservation;
                break;
            }
        }
        if (reusableReservation == null) {
            throw new SchedulerReportSet.ReservationRequestInvalidSlotException(reservationRequest, reservationInterval);
        }

        // Check the reusable reservation
        ReservationManager reservationManager = new ReservationManager(entityManager);
        List<ExistingReservation> existingReservations =
                reservationManager.getExistingReservations(reusableReservation, requestedSlot);
        applyAvailableReservations(existingReservations, ExistingReservation.class);
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
     * Type of objects in the {@link Savepoint#changes}
     */
    private static enum ObjectType
    {
        /**
         * {@link SchedulerContext#referencedResources}
         */
        REFERENCED_RESOURCE,

        /**
         * {@link SchedulerContext#allocatedReservations}
         */
        ALLOCATED_RESERVATION,

        /**
         * {@link SchedulerContext#availableReservations}
         * {@link SchedulerContext#availableExecutables}
         */
        AVAILABLE_RESERVATION
    }

    /**
     * State of objects in the {@link Savepoint#changes}
     */
    private static enum ObjectState
    {
        /**
         * Object has been added to the {@link SchedulerContext}.
         */
        ADDED,

        /**
         * Object has been removed from the {@link SchedulerContext}.
         */
        REMOVED
    }

    /**
     * {@link RangeSet.Bucket} for {@link RoomReservation}s.
     */
    private static class RoomBucket extends RangeSet.Bucket<DateTime, RoomReservation>
    {
        /**
         * Sum of {@link RoomReservation#getLicenseCount()}
         */
        private int licenseCount = 0;

        /**
         * Constructor.
         *
         * @param rangeValue
         */
        public RoomBucket(DateTime rangeValue)
        {
            super(rangeValue);
        }

        /**
         * @return {@link #licenseCount}
         */
        private int getLicenseCount()
        {
            return licenseCount;
        }

        @Override
        public boolean add(RoomReservation roomReservation)
        {
            if (super.add(roomReservation)) {
                this.licenseCount += roomReservation.getLicenseCount();
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public boolean remove(Object object)
        {
            if (super.remove(object)) {
                RoomReservation roomReservation = (RoomReservation) object;
                this.licenseCount -= roomReservation.getLicenseCount();
                return true;
            }
            else {
                return false;
            }
        }
    }
}
