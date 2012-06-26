package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.common.api.DateTimeSlot;
import cz.cesnet.shongo.common.api.Period;
import cz.cesnet.shongo.common.api.SecurityToken;
import cz.cesnet.shongo.common.xmlrpc.FaultException;
import cz.cesnet.shongo.common.xmlrpc.Service;

import java.util.Map;

/**
 * Interface to the service handling operations on reservations.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface ReservationService extends Service
{
    /**
     * Creates a new reservation request.
     * <p/>
     * The user with the given token will be the resource owner.
     *
     * @param token      token of the user requesting the operation
     * @param type
     * @param attributes map of reservation attributes; should only contain attributes specified in the
     *                   {@link ReservationRequest} class while all the attributes marked as required must be present
     * @return the created reservation request auto-generated identifier
     */
    public String createReservationRequest(SecurityToken token, ReservationRequestType type, Map attributes)
            throws FaultException;

    /**
     * Modifies a given reservation.
     *
     * @param token         token of the user requesting the operation
     * @param reservationId Shongo identifier of the reservation to modify
     * @param attributes    map of reservation attributes to change
     */
    public void modifyReservationRequest(SecurityToken token, String reservationId, Map attributes)
            throws FaultException;

    /**
     * Deletes a given reservation.
     *
     * @param token         token of the user requesting the operation
     * @param reservationId Shongo identifier of the reservation to modify
     */
    public void deleteReservationRequest(SecurityToken token, String reservationId) throws FaultException;

    /**
     * Gets the complete Reservation object.
     *
     * @param token         token of the user requesting the operation
     * @param reservationId Shongo identifier of the reservation to get
     */
    //public ReservationRequest getReservation(SecurityToken token, String reservationId);

    /**
     * Lists all the time slots with assigned resources that were allocated by the scheduler for the reservation.
     *
     * @param token         token of the user requesting the operation
     * @param reservationId Shongo identifier of the reservation to get
     * @return
     */
    //public ReservationAllocation getReservationAllocation(SecurityToken token, String reservationId);

    /**
     * Lists resources allocated by a given reservation in a given time slot, matching a filter.
     *
     * @param token         token of the user requesting the operation
     * @param reservationId Shongo identifier of the reservation to get
     * @param slot
     * @param filter
     * @return
     */
    //public ResourceSummary[] listReservationResources(SecurityToken token, String reservationId, DateTimeSlot slot,
    //        Map filter);

    /**
     * Lists all the reservations matching a filter.
     *
     * @param token  token of the user requesting the operation
     * @param filter
     * @return
     */
    //public ReservationSummary[] listReservations(SecurityToken token, Map filter);

    /**
     * Looks up available time slots for a given reservation duration and resources.
     *
     * @param token
     * @param duration
     * @param resources
     * @param interDomain specification whether inter-domain lookup should be performed
     * @return
     */
    //public DateTimeSlot[] findReservationAvailableTime(SecurityToken token, Period duration, Resource[] resources,
    //        boolean interDomain);
}
