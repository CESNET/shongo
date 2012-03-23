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
     * @param attributes
     * @return reservation id
     */
    public Reservation createReservation(SecurityToken token, AttributeMap<Reservation> attributes) throws XmlRpcException {

        Reservation reservationAttributes = attributes.getObject();
        if ( reservationAttributes.getType() == null )
            throw new FaultException(Fault.ReservationType_NotFilled);
        if ( reservationAttributes.getDate() == null )
            throw new FaultException(Fault.Date_NotFilled);
        if ( reservationAttributes.getType() == ReservationType.Periodic
                && (reservationAttributes.getDate() instanceof PeriodicDate) == false)
            throw new FaultException(Fault.PeriodicDate_Required);

        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID().toString());
        reservation.setType(reservationAttributes.getType());
        reservation.setDate(reservationAttributes.getDate());
        return reservation;
    }
    
    public Reservation modifyReservation(SecurityToken token, String id, AttributeMap<Reservation> attributes) throws FaultException {
        if ( id.equals("15082783-5b6f-4287-9015-3dbc0ab2f0d9") == false )
            throw new FaultException(Fault.Reservation_NotFound, id);

        Reservation reservation = new Reservation();
        reservation.setId("15082783-5b6f-4287-9015-3dbc0ab2f0d9");
        reservation.setType(ReservationType.OneTime);
        reservation.setDescription("First test reservation");

        attributes.populateObject(reservation);

        return reservation;
    }

    /**
     * List existing reservations
     *
     * @param token
     * @return reservations
     */
    public Reservation[] listReservations(SecurityToken token) {
        return listReservations(token, new AttributeMap(Reservation.class));
    }

    /**
     * List existing reservations with filter
     *
     * @param token
     * @param filter
     * @return reservations
     */
    public Reservation[] listReservations(SecurityToken token, AttributeMap<Reservation> filter) {
        ArrayList<Reservation> reservations = new ArrayList<Reservation>();

        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID().toString());
        reservation.setType(ReservationType.OneTime);
        reservations.add(reservation);

        return reservations.toArray(new Reservation[]{});
    }
}
