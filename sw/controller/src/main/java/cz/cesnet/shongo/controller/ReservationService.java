package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.*;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Reservation service
 *
 * @author Martin Srom
 */
public class ReservationService
{
    /**
     * Create reservation
     *
     * @param token
     * @param reservation
     * @return reservation id
     */
    public Reservation createReservation(SecurityToken token, Reservation reservation) {
        reservation.setId(UUID.randomUUID().toString());
        return reservation;
    }

    /**
     * List existing reservations
     *
     * @param token
     * @return reservations
     */
    public Reservation[] listReservations(SecurityToken token) {
        return listReservations(token, new Reservation());
    }

    /**
     * List existing reservations with filter
     *
     * @param token
     * @param filter
     * @return reservations
     */
    public Reservation[] listReservations(SecurityToken token, Reservation filter) {
        ArrayList<Reservation> reservations = new ArrayList<Reservation>();

        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID().toString());
        reservation.setType(ReservationType.OneTime);
        reservations.add(reservation);

        return reservations.toArray(new Reservation[]{});
    }
}
