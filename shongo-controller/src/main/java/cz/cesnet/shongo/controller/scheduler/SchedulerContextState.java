package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability;
import cz.cesnet.shongo.controller.booking.alias.AliasReservation;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.reservation.TargetedReservation;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.resource.ResourceReservation;
import cz.cesnet.shongo.controller.booking.value.ValueReservation;
import cz.cesnet.shongo.controller.booking.value.provider.ValueProvider;
import cz.cesnet.shongo.controller.notification.AbstractNotification;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * {@link SchedulerContext} current state.
 */
public class SchedulerContextState
{
    private static Logger logger = LoggerFactory.getLogger(SchedulerContextState.class);

    /**
     * Current {@link Savepoint} to which are recorded all performed changes.
     */
    private Savepoint currentSavepoint;

    /**
     * Specifies whether new {@link #notifications} can be added.
     */
    private boolean notificationsEnabled = true;

    /**
     * List of created {@link AbstractNotification}s.
     */
    private List<AbstractNotification> notifications = new LinkedList<AbstractNotification>();

    /**
     * Set of resources referenced from {@link ResourceReservation}s in the context.
     */
    private Set<Resource> referencedResources = new HashSet<Resource>();

    /**
     * Set of newly allocated {@link Reservation}s which must be consider (the allocated resource should not
     * be considered as available).
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
     * Map of {@link cz.cesnet.shongo.controller.scheduler.AvailableExecutable}s by {@link Executable}s.
     */
    private Map<Executable, AvailableExecutable> availableExecutables = new HashMap<Executable, AvailableExecutable>();

    /**
     * {@link cz.cesnet.shongo.controller.scheduler.ReservationTransaction} by {@link Reservation} class.
     */
    private Map<Class<? extends TargetedReservation>, ReservationTransaction<TargetedReservation>> reservationTransactionByType =
            new HashMap<Class<? extends TargetedReservation>, ReservationTransaction<TargetedReservation>>();

    /**
     * List of {@link ReservationRequest} which should be forced to be reallocated.
     */
    private List<ReservationRequest> forceReservationRequestReallocation = new LinkedList<ReservationRequest>();

    /**
     * List of {@link ReservationRequest} which should be attempted to be reallocated.
     */
    private List<ReservationRequest> tryReservationRequestReallocation = new LinkedList<ReservationRequest>();

    /**
     * List of {@link Reservation}s which should be deleted.
     */
    private List<Reservation> reservationsToDelete = new LinkedList<Reservation>();

    /**
     * @return {@link #currentSavepoint}
     */
    public Savepoint getCurrentSavepoint()
    {
        return currentSavepoint;
    }

    /**
     * @param notificationsEnabled sets the {@link #notificationsEnabled}
     */
    public void enableNotifications(boolean notificationsEnabled)
    {
        this.notificationsEnabled = notificationsEnabled;
    }

    /**
     * @return {@link #notifications}
     */
    public List<AbstractNotification> getNotifications()
    {
        return Collections.unmodifiableList(notifications);
    }

    /**
     * @param notification to be added to the {@link #notifications}
     */
    public void addNotification(AbstractNotification notification)
    {
        if (!notificationsEnabled) {
            return;
        }
        if (notifications.add(notification)) {
            onChange(ObjectType.NOTIFICATION, notification, ObjectState.ADDED);
        }
    }

    /**
     * @param notifications to be added to the {@link #notifications}
     */
    public void addNotifications(List<AbstractNotification> notifications)
    {
        if (!notificationsEnabled) {
            return;
        }
        for (AbstractNotification notification : notifications) {
            addNotification(notification);
        }
    }

    /**
     * @param notification to be removed from the {@link #notifications}
     */
    public void removeNotification(AbstractNotification notification)
    {
        if (notifications.remove(notification)) {
            onChange(ObjectType.NOTIFICATION, notification, ObjectState.REMOVED);
        }
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
     * Remove all {@link #referencedResources}.
     */
    public void clearReferencedResources()
    {
        if (currentSavepoint != null) {
            throw new TodoImplementException("Clear referenced resources when savepoint is active.");
        }
        referencedResources.clear();
    }

    /**
     * @return {@link #allocatedReservations}
     */
    public Set<Reservation> getAllocatedReservations()
    {
        return Collections.unmodifiableSet(allocatedReservations);
    }

    /**
     * @param reservation to be added to the {@link SchedulerContextState} as already allocated.
     */
    public void addAllocatedReservation(Reservation reservation)
    {
        if (!allocatedReservations.add(reservation)) {
            // Reservation is already added as allocated to the context
            return;
        }
        onChange(ObjectType.ALLOCATED_RESERVATION, reservation, ObjectState.ADDED);

        if (reservation instanceof TargetedReservation) {
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
     * @param reservation to be removed from the {@link SchedulerContextState} as already allocated.
     */
    public void removeAllocatedReservation(Reservation reservation)
    {
        if (!allocatedReservations.remove(reservation)) {
            // Reservation is not added as allocated to the context
            return;
        }
        onChange(ObjectType.ALLOCATED_RESERVATION, reservation, ObjectState.REMOVED);

        if (reservation instanceof TargetedReservation) {
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
     * @param objectId        for which the {@link AvailableReservation}s should be returned
     * @param slot
     * @param reservationType
     * @return {@link AvailableReservation}s ({@link ResourceReservation}s) for given {@code resource}
     */
    public <T extends TargetedReservation> Set<AvailableReservation<T>> getAvailableReservations(Long objectId,
            Interval slot, Class<T> reservationType)
    {
        @SuppressWarnings("unchecked")
        ReservationTransaction<T> reservationTransaction = (ReservationTransaction<T>)
                reservationTransactionByType.get(getReservationTransactionType(reservationType));
        if (reservationTransaction == null) {
            return Collections.emptySet();
        }
        return reservationTransaction.getAvailableReservations(objectId, slot);
    }

    /**
     * @param aliasProvider
     * @param slot
     * @return collection of {@link AvailableReservation}s ({@link cz.cesnet.shongo.controller.booking.alias.AliasReservation}s) for given {@code aliasProvider}
     */
    public Collection<AvailableReservation<AliasReservation>> getAvailableAliasReservations(
            AliasProviderCapability aliasProvider, Interval slot)
    {
        return getAvailableReservations(aliasProvider.getId(), slot, AliasReservation.class);
    }

    /**
     * @param resource for which the {@link AvailableReservation}s should be returned
     *                 @param slot
     * @return {@link AvailableReservation}s ({@link ResourceReservation}s) for given {@code resource}
     */
    public Set<AvailableReservation<ResourceReservation>> getAvailableResourceReservations(
            Resource resource, Interval slot)
    {
        return getAvailableReservations(resource.getId(), slot, ResourceReservation.class);
    }

    /**
     * @param valueProvider for which the {@link AvailableReservation}s should be returned
     *                      @param slot
     * @return {@link AvailableReservation}s ({@link cz.cesnet.shongo.controller.booking.value.ValueReservation}s) for given {@code valueProvider}
     */
    public Set<AvailableReservation<ValueReservation>> getAvailableValueReservations(
            ValueProvider valueProvider, Interval slot)
    {
        return getAvailableReservations(valueProvider.getId(), slot, ValueReservation.class);
    }

    /**
     * @param originalReservation to be added to the {@link SchedulerContextState} as available
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
     * @param availableReservation to be added to the {@link SchedulerContextState}
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
     * @param availableReservation to be removed from the {@link SchedulerContextState}
     */
    public void removeAvailableReservation(AvailableReservation<? extends Reservation> availableReservation)
    {
        removeAvailableReservation(availableReservation, true, true);
    }

    /**
     * @param availableReservation to be removed from the {@link SchedulerContextState}
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
     * @return new {@link Savepoint} for this {@link SchedulerContextState}
     */
    public Savepoint createSavepoint()
    {
        Savepoint savepoint = new Savepoint();
        savepoint.previousSavepoint = currentSavepoint;
        if (currentSavepoint != null) {
            currentSavepoint.nextSavepoint = savepoint;
        }
        currentSavepoint = savepoint;
        return savepoint;
    }

    /**
     * Apply {@link #reservationTransactionByType} to given {@code reservations}.
     *
     * @param resourceId
     * @param reservations
     */
    public <T extends TargetedReservation> void applyReservations(Long resourceId, Interval slot, List<T> reservations,
            Class<T> reservationType)
    {
        @SuppressWarnings("unchecked")
        ReservationTransaction<T> reservationTransaction = (ReservationTransaction<T>)
                reservationTransactionByType.get(getReservationTransactionType(reservationType));
        if (reservationTransaction != null) {
            reservationTransaction.applyReservations(resourceId, slot, reservations);
        }
    }

    /**
     * Apply {@link #reservationTransactionByType} to given map of {@link ValueReservation}s.
     *
     * @param resourceId
     * @param valueReservations
     */
    public <T extends TargetedReservation> void applyValueReservations(Long resourceId, Interval slot, Map<Long, Map.Entry<String, Interval>> valueReservations)
    {
        @SuppressWarnings("unchecked")
        ReservationTransaction<T> reservationTransaction = (ReservationTransaction<T>)
                reservationTransactionByType.get(getReservationTransactionType(ValueReservation.class));
        if (reservationTransaction != null) {
            reservationTransaction.removeSeparateReservations(resourceId, slot, valueReservations);
            List<ValueReservation> overlapsReservations = reservationTransaction.getAllocatedOverlapsReservations(resourceId, slot, ValueReservation.class);
            for (ValueReservation reservation : overlapsReservations) {
                Map.Entry<String, Interval> value = new AbstractMap.SimpleEntry<>(reservation.getValue(), reservation.getSlot());
                valueReservations.put(reservation.getId(), value);
            }
        }
    }

    /**
     * @return {@link #reservationsToDelete}
     */
    public List<Reservation> getReservationsToDelete()
    {
        return reservationsToDelete;
    }

    /**
     * @param reservation to be deleted
     */
    public void addReservationToDelete(Reservation reservation)
    {
        reservationsToDelete.add(reservation);
    }

    /**
     * @return iterator of {@link #forceReservationRequestReallocation}
     */
    public List<ReservationRequest> getForceReallocation()
    {
        return Collections.unmodifiableList(forceReservationRequestReallocation);
    }

    /**
     * @return iterator of {@link #tryReservationRequestReallocation}
     */
    public Iterator<ReservationRequest> getTryReallocationIterator()
    {
        return tryReservationRequestReallocation.iterator();
    }

    /**
     * @param reservationRequest to be forced to be reallocated
     */
    public void forceReservationRequestReallocation(ReservationRequest reservationRequest)
    {
        forceReservationRequestReallocation.add(reservationRequest);
    }

    /**
     * @param reservationRequest to be tried to be reallocated
     */
    public void tryReservationRequestReallocation(ReservationRequest reservationRequest)
    {
        tryReservationRequestReallocation.add(reservationRequest);
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
     * Represents a savepoint for the {@link SchedulerContextState} to which it can be reverted.
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
            try {

                // Revert changes
                for (ObjectType objectType : changes.keySet()) {
                    Map<Object, ObjectState> objectTypeChanges = changes.get(objectType);
                    switch (objectType) {
                        case NOTIFICATION:
                            for (Object object : objectTypeChanges.keySet()) {
                                ObjectState objectState = objectTypeChanges.get(object);
                                if (objectState == ObjectState.ADDED) {
                                    removeNotification((AbstractNotification) object);
                                }
                                else {
                                    addNotification((AbstractNotification) object);
                                }
                            }
                            break;
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
            }
            finally {
                // Restore current savepoint
                currentSavepoint = storedCurrentSavepoint;
            }

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
         * {@link SchedulerContextState#notifications}
         */
        NOTIFICATION,

        /**
         * {@link SchedulerContextState#referencedResources}
         */
        REFERENCED_RESOURCE,

        /**
         * {@link SchedulerContextState#allocatedReservations}
         */
        ALLOCATED_RESERVATION,

        /**
         * {@link SchedulerContextState#availableReservations}
         * {@link SchedulerContextState#availableExecutables}
         */
        AVAILABLE_RESERVATION
    }

    /**
     * State of objects in the {@link Savepoint#changes}
     */
    private static enum ObjectState
    {
        /**
         * Object has been added to the {@link SchedulerContextState}.
         */
        ADDED,

        /**
         * Object has been removed from the {@link SchedulerContextState}.
         */
        REMOVED
    }
}
