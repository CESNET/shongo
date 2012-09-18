package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.controller.api.xmlrpc.Service;
import cz.cesnet.shongo.fault.FaultException;

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
     * @return the created reservation request identifier
     */
    @API
    public String createReservationRequest(SecurityToken token, AbstractReservationRequest reservationRequest)
            throws FaultException;

    /**
     * Modifies a given reservation.
     *
     * @param token              token of the user requesting the operation
     * @param reservationRequest reservation request with attributes to be modified
     */
    @API
    public void modifyReservationRequest(SecurityToken token, AbstractReservationRequest reservationRequest)
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
     * @return collection of reservation requests
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
    public AbstractReservationRequest getReservationRequest(SecurityToken token, String reservationRequestIdentifier)
            throws FaultException;

    /**
     * @param token
     * @param reservationRequestIdentifier
     * @return collection of already allocated compartments for given reservation request
     * @throws FaultException
     */
    @API
    public Collection<Reservation> listReservations(SecurityToken token,
            String reservationRequestIdentifier) throws FaultException;
}
