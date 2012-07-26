package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.FaultException;
import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.controller.api.xmlrpc.Service;

import java.util.Collection;

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
     * The user with the given {@code token} will be the request owner.
     *
     * @param token              token of the user requesting the operation
     * @param reservationRequest reservation request; should contains all attributes marked as {@link Required}
     *                           in {@link ReservationRequest}
     * @return the created reservation request identifier
     */
    @API
    public String createReservationRequest(SecurityToken token, ReservationRequest reservationRequest)
            throws FaultException;

    /**
     * Modifies a given reservation.
     *
     * @param token              token of the user requesting the operation
     * @param reservationRequest reservation request with attributes to be modified
     */
    @API
    public void modifyReservationRequest(SecurityToken token, ReservationRequest reservationRequest)
            throws FaultException;

    /**
     * Deletes a given reservation.
     *
     * @param token                        token of the user requesting the operation
     * @param reservationRequestIdentifier Shongo identifier of the reservation to modify
     */
    @API
    public void deleteReservationRequest(SecurityToken token, String reservationRequestIdentifier)
            throws FaultException;

    /**
     * Lists all the reservation requests.
     *
     * @param token token of the user requesting the operation
     * @return array of reservation requests
     */
    @API
    public Collection<ReservationRequestSummary> listReservationRequests(SecurityToken token);

    /**
     * Gets the complete Reservation object.
     *
     * @param token                        token of the user requesting the operation
     * @param reservationRequestIdentifier identifier of the reservation request to get
     */
    @API
    public ReservationRequest getReservationRequest(SecurityToken token, String reservationRequestIdentifier)
            throws FaultException;

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
