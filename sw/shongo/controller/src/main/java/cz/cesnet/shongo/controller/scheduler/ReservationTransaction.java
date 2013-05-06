package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.reservation.Reservation;

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
     * @param reservation to be removed from the {@link ReservationTransaction}'s provided {@link cz.cesnet.shongo.controller.reservation.Reservation}s
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
     * @return set of provided {@link cz.cesnet.shongo.controller.reservation.Reservation}s for object with given {@code objectId}
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
    <T extends Reservation> void applyReservations(Long objectId, Collection<T> reservations)
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
