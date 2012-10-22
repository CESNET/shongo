package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.util.RangeSet;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Represents an abstract cache. Cache holds only reservations inside the {@link #workingInterval} If the
 * {@link #workingInterval} isn't set the cache of reservations will be empty (it is not desirable to load all
 * reservations for the entire time span).
 *
 * @param <T> type of cached object
 * @param <R> type of reservation for cached object
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractReservationCache<T extends PersistentObject, R extends Reservation>
        extends AbstractCache<T>
{
    /**
     * Working interval for which are loaded allocated virtual rooms.
     */
    private Interval workingInterval;

    /**
     * Represents a reference data time which is a rounded now().
     */
    private DateTime referenceDateTime;

    /**
     * Map of cached object states by theirs identifiers.
     */
    private Map<Long, ObjectState<R>> objectStateById = new HashMap<Long, ObjectState<R>>();

    /**
     * @return {@link #workingInterval}
     */
    public Interval getWorkingInterval()
    {
        return workingInterval;
    }

    /**
     * @return {@link #referenceDateTime}
     */
    public DateTime getReferenceDateTime()
    {
        if (referenceDateTime == null) {
            return DateTime.now();
        }
        return referenceDateTime;
    }

    /**
     * @param workingInterval   sets the {@link #workingInterval}
     * @param referenceDateTime sets the {@link #referenceDateTime}
     * @param entityManager     used for reloading reservations of resources for the new interval
     */
    public void setWorkingInterval(Interval workingInterval, DateTime referenceDateTime, EntityManager entityManager)
    {
        if (!workingInterval.equals(this.workingInterval)) {
            this.workingInterval = workingInterval;
            if (referenceDateTime == null) {
                referenceDateTime = workingInterval.getStart();
            }
            this.referenceDateTime = referenceDateTime;
            workingIntervalChanged(entityManager);
        }
    }

    /**
     * Method called when {@link #workingInterval} is changed.
     *
     * @param entityManager
     */
    protected void workingIntervalChanged(EntityManager entityManager)
    {
        for (T object : getObjects()) {
            updateObjectState(object, entityManager);
        }
    }

    /**
     * @return new state for given {@code object}
     */
    protected ObjectState<R> createObjectState()
    {
        return new ObjectState<R>();
    }

    /**
     * @param object        to be added to the cache
     * @param entityManager for loading {@code object}'s state
     */
    public void addObject(T object, EntityManager entityManager)
    {
        super.addObject(object);

        Long objectId = object.getId();
        ObjectState<R> objectState = createObjectState();
        objectState.setObjectId(objectId);
        objectStateById.put(objectId, objectState);
        if (entityManager != null) {
            updateObjectState(object, entityManager);
        }
    }

    @Override
    public void addObject(T object)
    {
        addObject(object, null);
    }

    @Override
    public void removeObject(T object)
    {
        objectStateById.remove(object.getId());

        super.removeObject(object);
    }

    @Override
    public void clear()
    {
        objectStateById.clear();
        super.clear();
    }

    /**
     * @param object for which the state should be returned
     * @return state for given {@code object}
     * @throws IllegalArgumentException when state cannot be found
     */
    protected ObjectState<R> getObjectState(T object)
    {
        Long objectId = object.getId();
        ObjectState<R> objectState = objectStateById.get(objectId);
        if (objectState == null) {
            throw new IllegalArgumentException(
                    object.getClass().getSimpleName() + " '" + objectId + "' isn't in the cache!");
        }
        return objectState;
    }

    /**
     * @param object      for which the {@code reservation} is added
     * @param reservation to be added to the cache
     */
    public final void addReservation(T object, R reservation)
    {
        if (workingInterval != null && !reservation.getSlot().overlaps(workingInterval)) {
            return;
        }
        reservation.checkPersisted();
        onAddReservation(object, reservation);
    }

    /**
     * @param object      for which the {@code reservation} is added
     * @param reservation to be added to the cache
     */
    protected void onAddReservation(T object, R reservation)
    {
        ObjectState<R> objectState = getObjectState(object);
        objectState.addReservation(reservation);
    }

    /**
     * @param object      for which the {@code reservation} is removed
     * @param reservation to be removed from the cache
     */
    public final void removeReservation(T object, R reservation)
    {
        if (workingInterval != null && !reservation.getSlot().overlaps(workingInterval)) {
            return;
        }
        onRemove(object, reservation);
    }

    /**
     * @param object      for which the {@code reservation} is removed
     * @param reservation to be removed from the cache
     */
    public void onRemove(T object, R reservation)
    {
        ObjectState<R> objectState = getObjectState(object);
        objectState.removeReservation(reservation);
    }

    /**
     * Update {@code objectState} with given {@code entityManager}.
     *
     * @param object        for which should be state updated
     * @param entityManager which should be used for accessing database
     */
    private void updateObjectState(T object, EntityManager entityManager)
    {
        ObjectState<R> objectState = getObjectState(object);
        objectState.clear();

        if (workingInterval != null) {
            updateObjectState(object, workingInterval, entityManager);
        }
    }

    /**
     * Update {@code objectState} for given {@code workingInterval} with given {@code entityManager}.
     *
     * @param object          for which should be state updated
     * @param workingInterval for which the {@code objectState} should be updated
     * @param entityManager   which should be used for accessing database
     */
    protected abstract void updateObjectState(T object, Interval workingInterval,
            EntityManager entityManager);

    /**
     * Represents a cached object state.
     *
     * @param <R> type of reservation for cached object
     */
    public static class ObjectState<R extends Reservation>
    {
        /**
         * Identifier of the cached object.
         */
        private Long objectId;

        /**
         * {@link Reservation}s for the cached object.
         */
        private RangeSet<R, DateTime> reservations = new RangeSet<R, DateTime>();

        /**
         * Map of reservations by the identifier.
         */
        private Map<Long, R> reservationsById = new HashMap<Long, R>();

        /**
         * @return {@link #objectId}
         */
        public Long getObjectId()
        {
            return objectId;
        }

        /**
         * @param objectId sets the {@link #objectId}
         */
        public void setObjectId(Long objectId)
        {
            this.objectId = objectId;
        }

        /**
         * @param interval
         * @return list of reservations for cached object in given {@code interval}
         */
        public Set<R> getReservations(Interval interval)
        {
            return reservations.getValues(interval.getStart(), interval.getEnd());
        }

        /**
         * @param interval
         * @return list of reservations for cached object in given {@code interval}
         */
        public Set<R> getReservations(Interval interval, Transaction<R> transaction)
        {
            Set<R> reservations = getReservations(interval);
            if (transaction != null) {
                transaction.applyReservations(objectId, reservations);
            }
            return reservations;
        }

        /**
         * @param reservation to be added to the {@link ObjectState}
         */
        public void addReservation(R reservation)
        {
            // TODO: check if reservation doesn't collide

            Interval slot = reservation.getSlot();
            reservationsById.put(reservation.getId(), reservation);
            reservations.add(reservation, slot.getStart(), slot.getEnd());
        }

        /**
         * @param reservation to be removed from the {@link ObjectState}
         */
        public void removeReservation(R reservation)
        {
            Long reservationId = reservation.getId();
            reservation = reservationsById.get(reservationId);
            if (reservation == null) {
                throw new IllegalStateException("Reservation doesn't exist in the cache.");
            }
            reservations.remove(reservation);
            reservationsById.remove(reservationId);
        }

        /**
         * Clear all reservations from the {@link ObjectState}.
         */
        public void clear()
        {
            reservations.clear();
            reservationsById.clear();
        }
    }

    /**
     * Represents a transaction inside {@link AbstractReservationCache}.
     */
    public static class Transaction<R extends Reservation>
    {
        /**
         * Already allocated reservations in the {@link Transaction} (which make resources unavailable
         * for further reservations).
         */
        private Map<Long, Set<R>> allocatedReservationsByObjectId = new HashMap<Long, Set<R>>();

        /**
         * Provided reservations in the {@link Transaction} (which make resources available for further reservations).
         */
        private Map<Long, Set<R>> providedReservationsByObjectId = new HashMap<Long, Set<R>>();

        /**
         * @param objectId    for object for which the {@code reservation} is added
         * @param reservation to be added to the {@link Transaction} as allocated
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
         * @param reservation to be added to the {@link Transaction} as provided
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
         * @param reservation to be removed from the {@link Transaction}'s provided {@link Reservation}s
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
         * Apply {@link Transaction} to given {@code reservations} for given object with given {@code objectId}.
         *
         * @param objectId     for which the {@link Transaction} should apply
         * @param reservations to which the {@link Transaction} should apply
         */
        public void applyReservations(Long objectId, Collection<R> reservations)
        {
            Set<R> providedReservationsToApply = providedReservationsByObjectId.get(objectId);
            if (providedReservationsToApply != null) {
                Map<Long, R> reservationById = new HashMap<Long, R>();
                for (R reservation : reservations) {
                    reservationById.put(reservation.getId(), reservation);
                }
                for (R reservation : providedReservationsToApply) {
                    reservation = reservationById.get(reservation.getId());
                    if (reservation != null) {
                        reservations.remove(reservation);
                    }
                }
            }
            Set<R> allocatedReservationsToApply = allocatedReservationsByObjectId.get(objectId);
            if (allocatedReservationsToApply != null) {
                reservations.addAll(allocatedReservationsToApply);
            }
        }
    }
}
