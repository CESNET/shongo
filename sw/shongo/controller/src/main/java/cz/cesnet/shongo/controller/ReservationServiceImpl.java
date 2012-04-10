package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.common.api.Period;
import cz.cesnet.shongo.common.api.SecurityToken;
import cz.cesnet.shongo.common.api.TimeSlot;
import cz.cesnet.shongo.controller.api.*;

import java.util.Map;

/**
 * Reservation service implementation
 *
 * @author Martin Srom
 */
public class ReservationServiceImpl implements ReservationService
{
    /**
     * Creates a new reservation.
     * <p/>
     * The user with the given token will be the resource owner.
     *
     * @param token      token of the user requesting the operation
     * @param type
     * @param attributes map of reservation attributes; should only contain attributes specified in the Reservation
     *                   class while all the attributes marked as required must be present
     * @return the created reservation with auto-generated identifier
     */
    @Override
    public Reservation createReservation(SecurityToken token, ReservationType type, Map attributes)
    {
        throw new RuntimeException("TODO: Implement ReservationServiceImpl.createReservation");
    }

    /**
     * Modifies a given reservation.
     *
     * @param token         token of the user requesting the operation
     * @param reservationId Shongo identifier of the reservation to modify
     * @param attributes    map of reservation attributes to change
     */
    @Override
    public void modifyReservation(SecurityToken token, String reservationId, Map attributes)
    {
        throw new RuntimeException("TODO: Implement ReservationServiceImpl.modifyReservation");
    }

    /**
     * Deletes a given reservation.
     *
     * @param token         token of the user requesting the operation
     * @param reservationId Shongo identifier of the reservation to modify
     */
    @Override
    public void deleteReservation(SecurityToken token, String reservationId)
    {
        throw new RuntimeException("TODO: Implement ReservationServiceImpl.deleteReservation");
    }

    /**
     * Gets the complete Reservation object.
     *
     * @param token         token of the user requesting the operation
     * @param reservationId Shongo identifier of the reservation to get
     */
    @Override
    public Reservation getReservation(SecurityToken token, String reservationId)
    {
        throw new RuntimeException("TODO: Implement ReservationServiceImpl.getReservation");
    }

    /**
     * Lists all the time slots with assigned resources that were allocated by the scheduler for the reservation.
     *
     * @param token         token of the user requesting the operation
     * @param reservationId Shongo identifier of the reservation to get
     * @return
     */
    @Override
    public ReservationAllocation getReservationAllocation(SecurityToken token, String reservationId)
    {
        throw new RuntimeException("TODO: Implement ReservationServiceImpl.getReservationAllocation");
    }

    /**
     * Lists resources allocated by a given reservation in a given time slot, matching a filter.
     *
     * @param token         token of the user requesting the operation
     * @param reservationId Shongo identifier of the reservation to get
     * @param slot
     * @param filter
     * @return
     */
    @Override
    public ResourceSummary[] listReservationResources(SecurityToken token, String reservationId, TimeSlot slot,
            Map filter)
    {
        throw new RuntimeException("TODO: Implement ReservationServiceImpl.listReservationResources");
    }

    /**
     * Lists all the reservations matching a filter.
     *
     * @param token  token of the user requesting the operation
     * @param filter
     * @return
     */
    @Override
    public ReservationSummary[] listReservations(SecurityToken token, Map filter)
    {
        throw new RuntimeException("TODO: Implement ReservationServiceImpl.listReservations");
    }

    /**
     * Looks up available time slots for a given reservation duration and resources.
     *
     * @param token
     * @param duration
     * @param resources
     * @param interDomain specification whether inter-domain lookup should be performed
     * @return
     */
    @Override
    public TimeSlot[] findReservationAvailableTime(SecurityToken token, Period duration, Resource[] resources,
            boolean interDomain)
    {
        throw new RuntimeException("TODO: Implement ReservationServiceImpl.findReservationAvailableTime");
    }
}
