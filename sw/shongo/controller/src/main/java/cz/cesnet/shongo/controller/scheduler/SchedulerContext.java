package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.reservation.*;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
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
     * {@link cz.cesnet.shongo.controller.request.AbstractReservationRequest} for which the {@link Reservation} should be allocated.
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
     * Set of provided {@link Reservation}s.
     */
    private Set<Reservation> providedReservations = new HashSet<Reservation>();

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
     * Set of resources referenced from {@link ResourceReservation}s in the transaction.
     */
    private Set<Resource> referencedResources = new HashSet<Resource>();

    /**
     * Map of provided {@link cz.cesnet.shongo.controller.executor.Executable}s by
     * {@link Reservation} which allocates them.
     */
    private Map<Executable, Reservation> providedReservationByExecutable = new HashMap<Executable, Reservation>();

    /**
     * Map of provided {@link cz.cesnet.shongo.controller.executor.Executable}s by
     * {@link Reservation} which allocates them.
     */
    private Map<Long, Set<AliasReservation>> providedReservationsByAliasProviderId =
            new HashMap<Long, Set<AliasReservation>>();

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
     * @return {@link #allocatedReservations}
     */
    public Set<Reservation> getAllocatedReservations()
    {
        return Collections.unmodifiableSet(allocatedReservations);
    }

    /**
     * @return {@link #providedReservations}
     */
    public Set<Reservation> getProvidedReservations()
    {
        return Collections.unmodifiableSet(providedReservations);
    }

    /**
     * @param resource to be checked
     * @return true if given resource was referenced by any {@link ResourceReservation} added to the transaction,
     *         false otherwise
     */
    public boolean containsReferencedResource(Resource resource)
    {
        return referencedResources.contains(resource);
    }

    /**
     * @param executableType
     * @return collection of provided {@link cz.cesnet.shongo.controller.executor.Executable}s of given {@code executableType}
     */
    public <T extends Executable> Collection<T> getProvidedExecutables(Class<T> executableType)
    {
        Set<T> providedExecutables = new HashSet<T>();
        for (Executable providedExecutable : providedReservationByExecutable.keySet()) {
            if (executableType.isInstance(providedExecutable)) {
                providedExecutables.add(executableType.cast(providedExecutable));
            }
        }
        return providedExecutables;
    }

    /**
     * @param executable
     * @return provided {@link Reservation} for given {@code executable}
     */
    public Reservation getProvidedReservationByExecutable(Executable executable)
    {
        Reservation providedReservation = providedReservationByExecutable.get(executable);
        if (providedReservation == null) {
            throw new IllegalArgumentException("Provided reservation doesn't exists for given executable!");
        }
        return providedReservation;
    }

    /**
     * @param aliasProvider
     * @return collection of provided {@link AliasReservation}
     */
    public Collection<AliasReservation> getProvidedAliasReservations(AliasProviderCapability aliasProvider)
    {
        Long aliasProviderId = aliasProvider.getId();
        Set<AliasReservation> aliasReservations = providedReservationsByAliasProviderId.get(aliasProviderId);
        if (aliasReservations != null) {
            return aliasReservations;
        }
        return Collections.emptyList();
    }

    /**
     * @param resource for which the provided {@link ResourceReservation}s should be returned
     * @return provided {@link ResourceReservation}s for given {@code resource}
     */
    public Set<ResourceReservation> getProvidedResourceReservations(Resource resource)
    {
        return resourceReservationTransaction.getProvidedReservations(resource.getId());
    }

    /**
     * @param valueProvider for which the provided {@link ValueReservation}s should be returned
     * @return provided {@link ValueReservation}s for given {@code valueProvider}
     */
    public Set<ValueReservation> getProvidedValueReservations(ValueProvider valueProvider)
    {
        return valueReservationTransaction.getProvidedReservations(valueProvider.getId());
    }

    /**
     * @param reservation to be added to the {@link SchedulerContext} as already allocated.
     */
    public void addAllocatedReservation(Reservation reservation)
    {
        if (!allocatedReservations.add(reservation)) {
            // Reservation is already added as allocated to the transaction
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
            // Reservation is not added as allocated to the transaction
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
     * @param reservation to be added to the {@link SchedulerContext} as provided (the resources allocated by
     *                    the {@code reservation} are considered as available).
     */
    public void addProvidedReservation(Reservation reservation)
    {
        if (!providedReservations.add(reservation)) {
            // Reservation is already added as provided to the transaction
            return;
        }
        onChange(ObjectType.PROVIDED_RESERVATION, reservation, ObjectState.ADDED);

        if (reservation.getSlot().contains(getInterval())) {
            Executable executable = reservation.getExecutable();
            if (executable != null) {
                if (!providedReservationByExecutable.containsKey(executable)) {
                    providedReservationByExecutable.put(executable, reservation);
                }
            }

            if (reservation instanceof ExistingReservation) {
                throw new TodoImplementException("Providing already provided reservation is not implemented yet.");
                // It will be necessary to evaluate existing reservation to target reservation and to keep the
                // reference to the existing reservation.
                // In the end the existing reservation must be used as child reservation for any newly allocated
                // reservations and not the target reservation itself (a collision would occur).
            }
            else if (reservation instanceof ResourceReservation) {
                ResourceReservation resourceReservation = (ResourceReservation) reservation;
                resourceReservationTransaction.addProvidedReservation(
                        resourceReservation.getResource().getId(), resourceReservation);
            }
            else if (reservation instanceof ValueReservation) {
                ValueReservation valueReservation = (ValueReservation) reservation;
                valueReservationTransaction.addProvidedReservation(
                        valueReservation.getValueProvider().getId(), valueReservation);
            }
            else if (reservation instanceof RoomReservation) {
                RoomReservation roomReservation = (RoomReservation) reservation;
                roomReservationTransaction.addProvidedReservation(
                        roomReservation.getRoomProviderCapability().getId(), roomReservation);
            }
            else if (reservation instanceof AliasReservation) {
                AliasReservation aliasReservation = (AliasReservation) reservation;
                Long aliasProviderId = aliasReservation.getAliasProviderCapability().getId();
                Set<AliasReservation> aliasReservations = providedReservationsByAliasProviderId.get(aliasProviderId);
                if (aliasReservations == null) {
                    aliasReservations = new HashSet<AliasReservation>();
                    providedReservationsByAliasProviderId.put(aliasProviderId, aliasReservations);
                }
                aliasReservations.add(aliasReservation);
            }
        }

        // Add all child reservations
        for (Reservation childReservation : reservation.getChildReservations()) {
            addProvidedReservation(childReservation);
        }
    }

    /**
     * @param reservation to be removed from the provided {@link Reservation}s from the {@link SchedulerContext}
     */
    public void removeProvidedReservation(Reservation reservation)
    {
        if (!providedReservations.remove(reservation)) {
            // Reservation is not added as provided to the transaction
            return;
        }
        onChange(ObjectType.PROVIDED_RESERVATION, reservation, ObjectState.REMOVED);

        Executable executable = reservation.getExecutable();
        if (executable != null) {
            providedReservationByExecutable.remove(executable);
        }

        if (reservation instanceof ExistingReservation) {
            throw new TodoImplementException("Providing already provided reservation is not implemented yet.");
        }
        else if (reservation instanceof ResourceReservation) {
            ResourceReservation resourceReservation = (ResourceReservation) reservation;
            resourceReservationTransaction.removeProvidedReservation(
                    resourceReservation.getResource().getId(), resourceReservation);
        }
        else if (reservation instanceof ValueReservation) {
            ValueReservation aliasReservation = (ValueReservation) reservation;
            valueReservationTransaction.removeProvidedReservation(
                    aliasReservation.getValueProvider().getId(), aliasReservation);
        }
        else if (reservation instanceof RoomReservation) {
            RoomReservation roomReservation = (RoomReservation) reservation;
            roomReservationTransaction.removeProvidedReservation(
                    roomReservation.getRoomProviderCapability().getId(), roomReservation);
        }
        else if (reservation instanceof AliasReservation) {
            AliasReservation aliasReservation = (AliasReservation) reservation;
            Long aliasProviderId = aliasReservation.getAliasProviderCapability().getId();
            Set<AliasReservation> aliasReservations = providedReservationsByAliasProviderId.get(aliasProviderId);
            if (aliasReservations != null) {
                aliasReservations.remove(aliasReservation);
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
     * @param providedReservation
     * @return true whether given {@code providedReservation} is available in given {@code interval},
     *         false otherwise
     */
    public boolean isProvidedReservationAvailable(Reservation providedReservation)
    {
        ReservationManager reservationManager = new ReservationManager(entityManager);
        List<ExistingReservation> existingReservations =
                reservationManager.getExistingReservations(providedReservation, interval);
        return existingReservations.size() == 0;
    }

    /**
     * Find available alias in given {@code aliasProviderCapability}.
     *
     * @param valueProvider
     * @param requestedValue
     * @return available alias for given {@code interval} from given {@code aliasProviderCapability}
     */
    public AvailableValue getAvailableValue(ValueProvider valueProvider, String requestedValue)
            throws ValueProvider.InvalidValueException, ValueProvider.ValueAlreadyAllocatedException,
                   ValueProvider.NoAvailableValueException
    {
        // Find available alias value
        String value = null;
        // Provided value reservation by which the value is already allocated
        ValueReservation valueReservation = null;
        ValueProvider targetValueProvider = valueProvider.getTargetValueProvider();
        // Preferably use  provided alias
        Set<ValueReservation> providedValueReservations = getProvidedValueReservations(targetValueProvider);
        if (providedValueReservations.size() > 0) {
            if (requestedValue != null) {
                for (ValueReservation possibleValueReservation : providedValueReservations) {
                    if (possibleValueReservation.getValue().equals(requestedValue)) {
                        valueReservation = possibleValueReservation;
                        value = valueReservation.getValue();
                        break;
                    }
                }
            }
            else {
                valueReservation = providedValueReservations.iterator().next();
                value = valueReservation.getValue();
            }
        }
        // Else use generated value
        if (value == null) {
            ResourceManager resourceManager = new ResourceManager(entityManager);
            Long targetValueProviderId = targetValueProvider.getId();
            List<ValueReservation> allocatedValues = resourceManager.listValueReservationsInInterval(
                    targetValueProviderId, interval);
            applyValueReservations(targetValueProviderId, allocatedValues);
            Set<String> usedValues = new HashSet<String>();
            for (ValueReservation allocatedValue : allocatedValues) {
                usedValues.add(allocatedValue.getValue());
            }
            if (requestedValue != null) {
                value = valueProvider.generateValue(usedValues, requestedValue);
            }
            else {
                value = valueProvider.generateValue(usedValues);
            }
        }
        AvailableValue availableAlias = new AvailableValue();
        availableAlias.setValue(value);
        availableAlias.setValueReservation(valueReservation);
        return availableAlias;
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
        private Map<ObjectType, Map<Object, ObjectState>> changes = new HashMap<ObjectType, Map<Object, ObjectState>>();

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
                    case PROVIDED_RESERVATION:
                        for (Object object : objectTypeChanges.keySet()) {
                            ObjectState objectState = objectTypeChanges.get(object);
                            if (objectState == ObjectState.ADDED) {
                                removeProvidedReservation((Reservation) object);
                            }
                            else {
                                addProvidedReservation((Reservation) object);
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
         * {@link SchedulerContext#providedReservationByExecutable}
         * {@link SchedulerContext#providedReservationsByAliasProviderId}
         */
        PROVIDED_RESERVATION
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
