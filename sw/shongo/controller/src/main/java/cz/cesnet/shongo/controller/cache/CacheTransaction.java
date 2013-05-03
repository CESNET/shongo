package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.reservation.*;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.resource.value.ValueProvider;
import org.joda.time.Interval;

import java.util.*;

/**
 * Transaction for the {@link cz.cesnet.shongo.controller.Cache}.
 */
public class CacheTransaction
{
    /**
     * Interval for which the task is performed.
     */
    private final Interval interval;

    /**
     * {@link ReservationTransaction} for {@link ResourceReservation}s.
     */
    private ReservationTransaction<ResourceReservation> resourceReservationTransaction =
            new ReservationTransaction<ResourceReservation>();

    /**
     * {@link ReservationTransaction} for {@link ValueReservation}s.
     */
    private ReservationTransaction<ValueReservation> valueReservationTransaction =
            new ReservationTransaction<ValueReservation>();

    /**
     * {@link ReservationTransaction} for {@link RoomReservation}s.
     */
    private ReservationTransaction<RoomReservation> roomReservationTransaction =
            new ReservationTransaction<RoomReservation>();

    /**
     * Set of allocated {@link cz.cesnet.shongo.controller.reservation.Reservation}s.
     */
    private Set<Reservation> allocatedReservations = new HashSet<Reservation>();

    /**
     * Set of resources referenced from {@link ResourceReservation}s in the transaction.
     */
    private Set<Resource> referencedResources = new HashSet<Resource>();

    /**
     * Set of provided {@link cz.cesnet.shongo.controller.reservation.Reservation}s.
     */
    private Set<Reservation> providedReservations = new HashSet<Reservation>();

    /**
     * Map of provided {@link cz.cesnet.shongo.controller.executor.Executable}s by
     * {@link cz.cesnet.shongo.controller.reservation.Reservation} which allocates them.
     */
    private Map<Executable, Reservation> providedReservationByExecutable = new HashMap<Executable, Reservation>();

    /**
     * Map of provided {@link cz.cesnet.shongo.controller.executor.Executable}s by
     * {@link cz.cesnet.shongo.controller.reservation.Reservation} which allocates them.
     */
    private Map<Long, Set<AliasReservation>> providedReservationsByAliasProviderId =
            new HashMap<Long, Set<AliasReservation>>();

    /**
     * Current {@link Savepoint} to which are recorded all performed changes.
     */
    private Savepoint currentSavepoint;

    /**
     * Constructor.
     */
    public CacheTransaction(Interval interval)
    {
        this.interval = interval;
    }

    /**
     * @return {@link #interval}
     */
    public Interval getInterval()
    {
        return interval;
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
     * @return provided {@link cz.cesnet.shongo.controller.reservation.Reservation} for given {@code executable}
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
     * @param reservation to be added to the {@link CacheTransaction} as already allocated.
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
     * @param reservation to be removed from the {@link CacheTransaction} as already allocated.
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
     * @param reservation to be added to the {@link CacheTransaction} as provided (the resources allocated by
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
     * @param reservation to be removed from the provided {@link cz.cesnet.shongo.controller.reservation.Reservation}s from the {@link CacheTransaction}
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
     * @return new {@link Savepoint} for this {@link CacheTransaction}
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
     * Represents a transaction inside {@link AbstractReservationCache}.
     */
    public static class ReservationTransaction<R extends Reservation>
    {
        /**
         * Already allocated reservations in the {@link ReservationTransaction} (which make resources unavailable
         * for further reservations).
         */
        private Map<Long, Set<R>> allocatedReservationsByObjectId = new HashMap<Long, Set<R>>();

        /**
         * Provided reservations in the {@link ReservationTransaction} (which make resources available for further reservations).
         */
        private Map<Long, Set<R>> providedReservationsByObjectId = new HashMap<Long, Set<R>>();

        /**
         * @param objectId    for object for which the {@code reservation} is added
         * @param reservation to be added to the {@link ReservationTransaction} as allocated
         */
        public void addAllocatedReservation(Long objectId, R reservation)
        {
            Set<R> reservations = allocatedReservationsByObjectId.get(objectId);
            if (reservations == null) {
                reservations = new HashSet<R>();
                allocatedReservationsByObjectId.put(objectId, reservations);
            }
            reservations.add(reservation);
        }

        /**
         * @param objectId    for object for which the {@code reservation} is added
         * @param reservation to be removed from the {@link ReservationTransaction} as allocated
         */
        public void removeAllocatedReservation(Long objectId, R reservation)
        {
            Set<R> reservations = allocatedReservationsByObjectId.get(objectId);
            if (reservations == null) {
                return;
            }
            reservations.remove(reservation);
        }

        /**
         * @param objectId    for object for which the {@code reservation} is added
         * @param reservation to be added to the {@link ReservationTransaction} as provided
         */
        public void addProvidedReservation(Long objectId, R reservation)
        {
            Set<R> reservations = providedReservationsByObjectId.get(objectId);
            if (reservations == null) {
                reservations = new HashSet<R>();
                providedReservationsByObjectId.put(objectId, reservations);
            }
            reservations.add(reservation);
        }

        /**
         * @param objectId    for object for which the {@code reservation} is added
         * @param reservation to be removed from the {@link ReservationTransaction}'s provided {@link Reservation}s
         */
        public void removeProvidedReservation(Long objectId, R reservation)
        {
            Set<R> reservations = providedReservationsByObjectId.get(objectId);
            if (reservations != null) {
                reservations.remove(reservation);
            }
        }

        /**
         * @param objectId for object
         * @return set of provided {@link Reservation}s for object with given {@code objectId}
         */
        public Set<R> getProvidedReservations(Long objectId)
        {
            Set<R> reservations = providedReservationsByObjectId.get(objectId);
            if (reservations == null) {
                reservations = new HashSet<R>();
            }
            return reservations;
        }

        /**
         * Apply {@link ReservationTransaction} to given {@code reservations} for given object with given {@code objectId}.
         *
         * @param objectId     for which the {@link ReservationTransaction} should apply
         * @param reservations to which the {@link ReservationTransaction} should apply
         */
        private  <T extends Reservation> void applyReservations(Long objectId, Collection<T> reservations)
        {
            Set<R> providedReservationsToApply = providedReservationsByObjectId.get(objectId);
            if (providedReservationsToApply != null) {
                Map<Long, T> reservationById = new HashMap<Long, T>();
                for (T reservation : reservations) {
                    reservationById.put(reservation.getId(), reservation);
                }
                for (R providedReservation : providedReservationsToApply) {
                    Reservation reservation = reservationById.get(providedReservation.getId());
                    if (reservation != null) {
                        @SuppressWarnings("unchecked")
                        T typedReservation = (T) reservation;
                        reservations.remove(typedReservation);
                    }
                }
            }
            Set<R> allocatedReservationsToApply = allocatedReservationsByObjectId.get(objectId);
            if (allocatedReservationsToApply != null) {
                for (R reservation : allocatedReservationsToApply) {
                    @SuppressWarnings("unchecked")
                    T typedReservation = (T) reservation;
                    reservations.add(typedReservation);
                }
            }
        }
    }

    /**
     * Represents a savepoint for the {@link CacheTransaction} to which it can be reverted.
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
         * {@link CacheTransaction#referencedResources}
         */
        REFERENCED_RESOURCE,

        /**
         * {@link CacheTransaction#allocatedReservations}
         */
        ALLOCATED_RESERVATION,

        /**
         * {@link CacheTransaction#providedReservationByExecutable}
         * {@link CacheTransaction#providedReservationsByAliasProviderId}
         */
        PROVIDED_RESERVATION
    }

    /**
     * State of objects in the {@link Savepoint#changes}
     */
    private static enum ObjectState
    {
        /**
         * Object has been added to the {@link CacheTransaction}.
         */
        ADDED,

        /**
         * Object has been removed from the {@link CacheTransaction}.
         */
        REMOVED
    }
}
