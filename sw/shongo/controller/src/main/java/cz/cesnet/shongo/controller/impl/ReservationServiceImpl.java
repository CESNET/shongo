package cz.cesnet.shongo.controller.impl;

import cz.cesnet.shongo.common.api.DateTimeSlot;
import cz.cesnet.shongo.common.api.Period;
import cz.cesnet.shongo.common.api.SecurityToken;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;

import java.util.Map;

/**
 * Reservation service implementation
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationServiceImpl implements ReservationService
{
    @Override
    public String getServiceName()
    {
        return "Reservation";
    }

    @Override
    public String createReservation(SecurityToken token, ReservationType type, Map attributes)
    {
        throw new RuntimeException("TODO: Implement ReservationServiceImpl.createReservation");
    }

    @Override
    public void modifyReservation(SecurityToken token, String reservationId, Map attributes)
    {
        throw new RuntimeException("TODO: Implement ReservationServiceImpl.modifyReservation");
    }

    @Override
    public void deleteReservation(SecurityToken token, String reservationId)
    {
        throw new RuntimeException("TODO: Implement ReservationServiceImpl.deleteReservation");
    }

    @Override
    public Reservation getReservation(SecurityToken token, String reservationId)
    {
        throw new RuntimeException("TODO: Implement ReservationServiceImpl.getReservation");
    }

    @Override
    public ReservationAllocation getReservationAllocation(SecurityToken token, String reservationId)
    {
        throw new RuntimeException("TODO: Implement ReservationServiceImpl.getReservationAllocation");
    }

    @Override
    public ResourceSummary[] listReservationResources(SecurityToken token, String reservationId, DateTimeSlot slot,
            Map filter)
    {
        throw new RuntimeException("TODO: Implement ReservationServiceImpl.listReservationResources");
    }

    @Override
    public ReservationSummary[] listReservations(SecurityToken token, Map filter)
    {
        // TODO: reservation identifier should be computed only here
        throw new RuntimeException("TODO: Implement ReservationServiceImpl.listReservations");
    }

    @Override
    public DateTimeSlot[] findReservationAvailableTime(SecurityToken token, Period duration, Resource[] resources,
            boolean interDomain)
    {
        throw new RuntimeException("TODO: Implement ReservationServiceImpl.findReservationAvailableTime");
    }
}
