package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import org.joda.time.Interval;

import java.util.*;

/**
 * Represents a transaction for {@link Reservation}s.
 */
public class ReservationTransaction<R extends Reservation>
{
    /**
     * Already allocated reservations in the {@link ReservationTransaction} (which make resources unavailable
     * for further reservations).
     */
    private Map<Long, Set<R>> allocatedReservationsByObjectId = new HashMap<Long, Set<R>>();

    /**
     * {@link AvailableReservation}s in the {@link ReservationTransaction} (which make resources available for
     * further reservations).
     */
    private Map<Long, Set<AvailableReservation<R>>> availableReservationsByObjectId =
            new HashMap<Long, Set<AvailableReservation<R>>>();

    /**
     * Clear content of this {@link ReservationTransaction}.
     */
    public void clear()
    {
        allocatedReservationsByObjectId.clear();
        availableReservationsByObjectId.clear();
    }

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
     * @param objectId             for object for which the {@code availableReservation} is added
     * @param availableReservation to be added to the {@link ReservationTransaction}
     */
    public void addAvailableReservation(Long objectId, AvailableReservation<R> availableReservation)
    {
        Set<AvailableReservation<R>> reservations = availableReservationsByObjectId.get(objectId);
        if (reservations == null) {
            reservations = new HashSet<AvailableReservation<R>>();
            availableReservationsByObjectId.put(objectId, reservations);
        }
        reservations.add(availableReservation);
    }

    /**
     * @param objectId             for object for which the {@code availableReservation} is added
     * @param availableReservation to be removed from the {@link ReservationTransaction}
     */
    public void removeAvailableReservation(Long objectId, AvailableReservation<R> availableReservation)
    {
        Set<AvailableReservation<R>> reservations = availableReservationsByObjectId.get(objectId);
        if (reservations != null) {
            reservations.remove(availableReservation);
        }
    }

    /**
     * @param objectId for object
     * @param slot
     * @return set of {@link AvailableReservation}s for object with given {@code objectId}
     */
    public Set<AvailableReservation<R>> getAvailableReservations(Long objectId, Interval slot)
    {
        Set<AvailableReservation<R>> possibleAvailableReservations = availableReservationsByObjectId.get(objectId);
        if (possibleAvailableReservations == null) {
            possibleAvailableReservations = Collections.emptySet();
        }
        Set<AvailableReservation<R>> availableReservations = new HashSet<AvailableReservation<R>>();
        for (AvailableReservation<R> availableReservation : possibleAvailableReservations) {
            if (!availableReservation.getOriginalReservation().getSlot().overlaps(slot)) {
                continue;
            }
            availableReservations.add(availableReservation);
        }
        return availableReservations;
    }

    /**
     * Apply {@link ReservationTransaction} to given {@code reservations} for given object with given {@code objectId}.
     *
     * @param objectId     for which the {@link ReservationTransaction} should apply
     * @param slot
     * @param reservations to which the {@link ReservationTransaction} should apply
     */
    <T extends Reservation> void applyReservations(Long objectId, Interval slot, Collection<T> reservations)
    {
        Set<AvailableReservation<R>> availableReservationsToApply = availableReservationsByObjectId.get(objectId);
        if (availableReservationsToApply != null) {
            Map<Long, T> reservationById = new HashMap<Long, T>();
            for (T reservation : reservations) {
                reservationById.put(reservation.getId(), reservation);
            }
            for (AvailableReservation<R> availableReservation : availableReservationsToApply) {
                if (!availableReservation.getOriginalReservation().getSlot().overlaps(slot)) {
                    continue;
                }
                Reservation reservation = reservationById.get(availableReservation.getTargetReservation().getId());
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
                if (!reservation.getSlot().overlaps(slot)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                T typedReservation = (T) reservation;
                reservations.add(typedReservation);
            }
        }
    }

    /**
     * Apply {@link ReservationTransaction} to given list of reservations by Ids for given object with given {@code objectId}.
     * ONLY removes available reservations which overlaps with given {@code slot}.
     *
     * @param objectId     for which the {@link ReservationTransaction} should apply
     * @param slot
     * @param reservationIds to which the {@link ReservationTransaction} should apply
     */
    public void removeSeparateReservations(Long objectId, Interval slot, Map<Long, ?> reservationIds)
    {
        Set<AvailableReservation<R>> availableReservationsToApply = availableReservationsByObjectId.get(objectId);
        if (availableReservationsToApply != null) {
            for (AvailableReservation<R> availableReservation : availableReservationsToApply) {
                if (!availableReservation.getOriginalReservation().getSlot().overlaps(slot)) {
                    continue;
                }
                Long availableReservationId = availableReservation.getTargetReservation().getId();
                if (reservationIds.containsKey(availableReservationId)) {
                    reservationIds.remove(availableReservationId);
                }
            }
        }
    }

    /**
     * Returns allocated reservations which overlaps with given {@code slot}
     *
     * @param objectId for which the {@link ReservationTransaction} should apply
     * @param slot
     * @param reservationType to which the {@link ReservationTransaction} should apply
     * @param <T> type of returned reservations
     * @return
     */
    public <T extends Reservation> List<T> getAllocatedOverlapsReservations(Long objectId, Interval slot, Class<T> reservationType)
    {
        Set<R> allocatedReservationsToApply = allocatedReservationsByObjectId.get(objectId);
        List<T> reservations = new ArrayList<>();
        if (allocatedReservationsToApply != null) {
            for (R reservation : allocatedReservationsToApply) {
                if (!reservation.getSlot().overlaps(slot)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                T typedReservation = (T) reservation;
                reservations.add(typedReservation);
            }
        }

        return reservations;
    }
}
