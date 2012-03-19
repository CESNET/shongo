package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.*;

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
     * @param date
     * @return reservation id
     */
    public String createReservation(Date date) {
        return date.toString();
    }
}
