package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.reservation.*;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import cz.cesnet.shongo.controller.resource.value.ValueProvider;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Context for the {@link cz.cesnet.shongo.controller.scheduler.ReservationTask}.
 */
public class SchedulerContext
{
    /**
     * {@link ReservationRequest} for which the {@link Reservation} should be allocated.
     */
    private ReservationRequest reservationRequest;

    /**
     * Interval for which the task is performed.
     */
    private final Interval interval;

    /**
     * @see cz.cesnet.shongo.controller.cache.Cache
     */
    private Cache cache;

    /**
     * Time which represents now.
     */
    private DateTime referenceDateTime = DateTime.now();

    /**
     * Entity manager.
     */
    private EntityManager entityManager;

    /**
     * @see cz.cesnet.shongo.controller.authorization.AuthorizationManager
     */
    private AuthorizationManager authorizationManager;

    /**
     * Set of allocated {@link Reservation}s.
     */
    private Set<Reservation> allocatedReservations = new HashSet<Reservation>();

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
     * {@link ReservationTransaction} for {@link cz.cesnet.shongo.controller.reservation.ResourceReservation}s.
     */
    private ReservationTransaction<ResourceReservation> resourceReservationTransaction =
            new ReservationTransaction<ResourceReservation>();

    /**
     * {@link ReservationTransaction} for {@link cz.cesnet.shongo.controller.reservation.ValueReservation}s.
     */
    private ReservationTransaction<ValueReservation> valueReservationTransaction =
            new ReservationTransaction<ValueReservation>();

    /**
     * {@link ReservationTransaction} for {@link cz.cesnet.shongo.controller.reservation.RoomReservation}s.
     */
    private ReservationTransaction<RoomReservation> roomReservationTransaction =
            new ReservationTransaction<RoomReservation>();

    /**
     * Set of resources referenced from {@link ResourceReservation}s in the context.
     */
    private Set<Resource> referencedResources = new HashSet<Resource>();

    /**
     * Map of {@link AvailableExecutable}s by {@link Executable}s.
     */
    private Map<Executable, AvailableExecutable> availableExecutables = new HashMap<Executable, AvailableExecutable>();

    /**
     * Map of {@link AvailableReservation}s ({@link AliasReservation}s) by {@link AliasProviderCapability} identifiers.
     */
    private Map<Long, Set<AvailableReservation<AliasReservation>>> availableReservationsByAliasProviderId =
            new HashMap<Long, Set<AvailableReservation<AliasReservation>>>();

    /**
     * Current {@link Savepoint} to which are recorded all performed changes.
     */
    private Savepoint currentSavepoint;

    /**
     * Constructor.
     *
     * @param cache    sets the {@link #cache}
     * @param interval sets the {@link #interval}
     */
    public SchedulerContext(Interval interval, Cache cache, EntityManager entityManager)
    {
        this.interval = interval;
        this.cache = cache;
        this.entityManager = entityManager;
    }

    /**
     * Constructor.
     *
     * @param reservationRequest sets the {@link #reservationRequest}
     * @param cache              sets the {@link #cache}
     * @param referenceDateTime  sets the {@link #referenceDateTime}
     * @param entityManager      which can be used
     */
    public SchedulerContext(ReservationRequest reservationRequest, Cache cache, DateTime referenceDateTime,
            EntityManager entityManager)
    {
        this(reservationRequest.getSlot(), cache, entityManager);
        this.reservationRequest = reservationRequest;
        this.referenceDateTime = referenceDateTime;
        if (entityManager != null) {
            this.authorizationManager = new AuthorizationManager(entityManager);
        }
    }

    /**
     * @return {@link #interval}
     */
    public Interval getInterval()
    {
        return interval;
    }

    /**
     * @return {@link #cache}
     */
    public Cache getCache()
    {
        return cache;
    }

    /**
     * @return {@link #referenceDateTime}
     */
    public DateTime getReferenceDateTime()
    {
        return referenceDateTime;
    }

    /**
     * @return {@link #entityManager}
     */
    public EntityManager getEntityManager()
    {
        return entityManager;
    }

    /**
     * @return description of {@link #reservationRequest}
     */
    public String getReservationDescription()
    {
        if (reservationRequest == null) {
            return null;
        }
        return reservationRequest.getDescription();
    }

    /**
     * @return true whether executables should be allocated,
     *         false otherwise
     */
    public boolean isExecutableAllowed()
    {
        return reservationRequest == null || reservationRequest.getPurpose().isExecutableAllowed();
    }

    /**
     * @return true whether only owned resource by the reservation request owner can be allocated,
     *         false otherwise
     */
    public boolean isOwnerRestricted()
    {
        return reservationRequest != null && reservationRequest.getPurpose().isByOwner();
    }

    /**
     * @return true whether maximum future and maximum duration should be checked,
     *         false otherwise
     */
    public boolean isMaximumFutureAndDurationRestricted()
    {
        return reservationRequest != null && !reservationRequest.getPurpose().isByOwner();
    }

    /**
     * @return collection of user-ids for reservation request owners
     */
    public Collection<String> getOwnerIds()
    {
        if (reservationRequest == null) {
            throw new IllegalStateException("Reservation request must not be null.");
        }
        if (authorizationManager == null) {
            throw new IllegalStateException("Authorization manager must not be null.");
        }
        Set<String> ownerIds = new HashSet<String>();
        EntityIdentifier reservationRequestId = new EntityIdentifier(reservationRequest);
        ownerIds.addAll(authorizationManager.getUserIdsWithRole(reservationRequestId, Role.OWNER));
        if (ownerIds.size() == 0) {
            ownerIds.add(reservationRequest.getUserId());
        }
        return ownerIds;
    }

    /**
     * @param resource whose owner should be checked
     * @return true if the {@link #reservationRequest} has an owner who is in the given {@code userIds},
     *         false otherwise
     */
    public boolean containsOwnerId(Resource resource)
    {
        Collection<String> reservationRequestOwnerIds = getOwnerIds();
        Set<String> resourceOwnerIds = new HashSet<String>();
        EntityIdentifier resourceId = new EntityIdentifier(resource);
        resourceOwnerIds.addAll(authorizationManager.getUserIdsWithRole(resourceId, Role.OWNER));
        if (resourceOwnerIds.size() == 0) {
            resourceOwnerIds.add(resource.getUserId());
        }
        return !Collections.disjoint(resourceOwnerIds, reservationRequestOwnerIds);
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
     * @return {@link #availableReservations}
     */
    public Set<AvailableReservation> getParentAvailableReservations()
    {
        Set<AvailableReservation> parentAvailableReservations = new HashSet<AvailableReservation>();
        for (AvailableReservation availableReservation : availableReservations) {
            Reservation originalReservation = availableReservation.getOriginalReservation();
            Reservation parentReservation = originalReservation.getParentReservation();
            if (parentReservation != null) {
                AvailableReservation parentAvailableReservation = getAvailableReservation(parentReservation);
                if (parentAvailableReservation == null) {
                    throw new IllegalArgumentException("Parent allocated reservation is not available.");
                }
                continue;
            }
            parentAvailableReservations.add(availableReservation);
        }
        return parentAvailableReservations;
    }

    /**
     * @param originalReservation for which the {@link AvailableReservation} should be returned
     * @return {@link AvailableReservation} for given {@code originalReservation}
     */
    public AvailableReservation<? extends Reservation> getAvailableReservation(Reservation originalReservation)
    {
        return availableReservationByOriginalReservation.get(originalReservation);
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
     * @param aliasProvider
     * @return collection of {@link AvailableReservation}s ({@link AliasReservation}s) for given {@code aliasProvider}
     */
    public Collection<AvailableReservation<AliasReservation>> getAvailableAliasReservations(
            AliasProviderCapability aliasProvider)
    {
        Long aliasProviderId = aliasProvider.getId();
        Set<AvailableReservation<AliasReservation>> availableReservations =
                availableReservationsByAliasProviderId.get(aliasProviderId);
        if (availableReservations != null) {
            return availableReservations;
        }
        return Collections.emptyList();
    }

    /**
     * @param resource for which the {@link AvailableReservation}s should be returned
     * @return {@link AvailableReservation}s ({@link ResourceReservation}s) for given {@code resource}
     */
    public Set<AvailableReservation<ResourceReservation>> getAvailableResourceReservations(Resource resource)
    {
        return resourceReservationTransaction.getAvailableReservations(resource.getId());
    }

    /**
     * @param valueProvider for which the {@link AvailableReservation}s should be returned
     * @return {@link AvailableReservation}s ({@link ValueReservation}s) for given {@code valueProvider}
     */
    public Set<AvailableReservation<ValueReservation>> getAvailableValueReservations(ValueProvider valueProvider)
    {
        return valueReservationTransaction.getAvailableReservations(valueProvider.getId());
    }

    /**
     * @param roomProvider for which the {@link AvailableReservation}s should be returned
     * @return {@link AvailableReservation}s ({@link RoomReservation}s) for given {@code roomProvider}
     */
    public Set<AvailableReservation<RoomReservation>> getAvailableRoomReservations(RoomProviderCapability roomProvider)
    {
        return roomReservationTransaction.getAvailableReservations(roomProvider.getId());
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

        if (reservation.getSlot().contains(getInterval())) {
            if (reservation instanceof ResourceReservation) {
                ResourceReservation resourceReservation = (ResourceReservation) reservation;
                Resource resource = resourceReservation.getResource();
                resourceReservationTransaction.addAllocatedReservation(resource.getId(), resourceReservation);
                addReferencedResource(resource);
            }
            else if (reservation instanceof ValueReservation) {
                ValueReservation valueReservation = (ValueReservation) reservation;
                valueReservationTransaction.addAllocatedReservation(
                        valueReservation.getValueProvider().getId(), valueReservation);
            }
            else if (reservation instanceof RoomReservation) {
                RoomReservation roomReservation = (RoomReservation) reservation;
                roomReservationTransaction.addAllocatedReservation(
                        roomReservation.getRoomProviderCapability().getId(), roomReservation);
            }
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

        if (reservation.getSlot().contains(getInterval())) {
            if (reservation instanceof ResourceReservation) {
                ResourceReservation resourceReservation = (ResourceReservation) reservation;
                Resource resource = resourceReservation.getResource();
                resourceReservationTransaction.removeAllocatedReservation(resource.getId(), resourceReservation);
                addReferencedResource(resource);
            }
            else if (reservation instanceof ValueReservation) {
                ValueReservation valueReservation = (ValueReservation) reservation;
                valueReservationTransaction.removeAllocatedReservation(
                        valueReservation.getValueProvider().getId(), valueReservation);
            }
            else if (reservation instanceof RoomReservation) {
                RoomReservation roomReservation = (RoomReservation) reservation;
                roomReservationTransaction.removeAllocatedReservation(
                        roomReservation.getRoomProviderCapability().getId(), roomReservation);
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
                throw new IllegalArgumentException("Reservation is already addded with different type.");
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

        Executable executable = targetReservation.getExecutable();
        if (executable != null) {
            if (!availableExecutables.containsKey(executable)) {
                availableExecutables.put(executable,
                        new AvailableExecutable<Executable>(executable, availableReservation));
            }
        }

        if (targetReservation instanceof ResourceReservation) {
            ResourceReservation resourceReservation = (ResourceReservation) targetReservation;
            resourceReservationTransaction.addAvailableReservation(resourceReservation.getResource().getId(),
                    availableReservation.cast(ResourceReservation.class));
        }
        else if (targetReservation instanceof ValueReservation) {
            ValueReservation valueReservation = (ValueReservation) targetReservation;
            valueReservationTransaction.addAvailableReservation(valueReservation.getValueProvider().getId(),
                    availableReservation.cast(ValueReservation.class));
        }
        else if (targetReservation instanceof RoomReservation) {
            RoomReservation roomReservation = (RoomReservation) targetReservation;
            roomReservationTransaction.addAvailableReservation(roomReservation.getRoomProviderCapability().getId(),
                    availableReservation.cast(RoomReservation.class));
        }
        else if (targetReservation instanceof AliasReservation) {
            AliasReservation aliasReservation = (AliasReservation) targetReservation;
            Long aliasProviderId = aliasReservation.getAliasProviderCapability().getId();
            Set<AvailableReservation<AliasReservation>> availableReservations =
                    availableReservationsByAliasProviderId.get(aliasProviderId);
            if (availableReservations == null) {
                availableReservations = new HashSet<AvailableReservation<AliasReservation>>();
                availableReservationsByAliasProviderId.put(aliasProviderId, availableReservations);
            }
            availableReservations.add(availableReservation.cast(AliasReservation.class));
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

        if (targetReservation instanceof ResourceReservation) {
            ResourceReservation resourceReservation = (ResourceReservation) targetReservation;
            resourceReservationTransaction.removeAvailableReservation(resourceReservation.getResource().getId(),
                    availableReservation.cast(ResourceReservation.class));
        }
        else if (targetReservation instanceof ValueReservation) {
            ValueReservation aliasReservation = (ValueReservation) targetReservation;
            valueReservationTransaction.removeAvailableReservation(aliasReservation.getValueProvider().getId(),
                    availableReservation.cast(ValueReservation.class));
        }
        else if (targetReservation instanceof RoomReservation) {
            RoomReservation roomReservation = (RoomReservation) targetReservation;
            roomReservationTransaction.removeAvailableReservation(roomReservation.getRoomProviderCapability().getId(),
                    availableReservation.cast(RoomReservation.class));
        }
        else if (targetReservation instanceof AliasReservation) {
            AliasReservation aliasReservation = (AliasReservation) targetReservation;
            Long aliasProviderId = aliasReservation.getAliasProviderCapability().getId();
            Set<AvailableReservation<AliasReservation>> availableReservations =
                    availableReservationsByAliasProviderId.get(aliasProviderId);
            if (availableReservations != null) {
                availableReservations.remove(availableReservation.cast(AliasReservation.class));
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
     * Apply {@link #resourceReservationTransaction} to given {@code resourceReservations}.
     *
     * @param resourceId
     * @param resourceReservations
     */
    public void applyResourceReservations(Long resourceId, List<ResourceReservation> resourceReservations)
    {
        resourceReservationTransaction.applyReservations(resourceId, resourceReservations);
    }

    /**
     * Apply {@link #roomReservationTransaction} to given {@code roomReservations}.
     *
     * @param roomProviderId
     * @param roomReservations
     */
    public void applyRoomReservations(Long roomProviderId, List<RoomReservation> roomReservations)
    {
        roomReservationTransaction.applyReservations(roomProviderId, roomReservations);
    }

    /**
     * Apply {@link #valueReservationTransaction} to given {@code valueReservations}.
     *
     * @param valueProviderId
     * @param valueReservations
     */
    public void applyValueReservations(Long valueProviderId, List<ValueReservation> valueReservations)
    {
        valueReservationTransaction.applyReservations(valueProviderId, valueReservations);
    }

    /**
     * @param reservation
     * @return true whether given {@code reservation} is available in given {@code interval} (it means it is not
     *         referenced by any {@link ExistingReservation}),
     *         false otherwise
     */
    public boolean isReservationAvailable(Reservation reservation)
    {
        ReservationManager reservationManager = new ReservationManager(entityManager);
        List<ExistingReservation> existingReservations =
                reservationManager.getExistingReservations(reservation, interval);
        return existingReservations.size() == 0;
    }

    /**
     * @param roomProviderCapability
     * @return {@link AvailableRoom} for given {@code roomProviderCapability} in given {@code interval}
     */
    public AvailableRoom getAvailableRoom(RoomProviderCapability roomProviderCapability)
    {
        int usedLicenseCount = 0;
        if (cache.getResourceCache().isResourceAvailable(roomProviderCapability.getResource(), this)) {

            ReservationManager reservationManager = new ReservationManager(entityManager);
            List<RoomReservation> roomReservations =
                    reservationManager.getRoomReservations(roomProviderCapability, interval);
            applyRoomReservations(roomProviderCapability.getId(), roomReservations);
            for (RoomReservation roomReservation : roomReservations) {
                usedLicenseCount += roomReservation.getRoomConfiguration().getLicenseCount();
            }
        }
        else {
            usedLicenseCount = roomProviderCapability.getLicenseCount();
        }
        AvailableRoom availableRoom = new AvailableRoom();
        availableRoom.setRoomProviderCapability(roomProviderCapability);
        availableRoom.setMaximumLicenseCount(roomProviderCapability.getLicenseCount());
        availableRoom.setAvailableLicenseCount(roomProviderCapability.getLicenseCount() - usedLicenseCount);
        return availableRoom;
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
                        throw new TodoImplementException(objectType.toString());
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
}
