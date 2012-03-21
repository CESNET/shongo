package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.*;
import org.apache.xmlrpc.XmlRpcException;

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
    public Reservation createReservation(SecurityToken token, Reservation reservation) throws XmlRpcException {
        if ( reservation.getType() == null )
            throw new FaultException(Fault.ReservationType_NotFilled);
        if ( reservation.getDate() == null )
            throw new FaultException(Fault.Date_NotFilled);
        if ( reservation.getType() == ReservationType.Periodic && (reservation.getDate() instanceof PeriodicDate) == false)
            throw new FaultException(Fault.PeriodicDate_Required);
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
