package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.*;
import org.joda.time.Interval;

/**
 * Interface to the service handling operations on reservations.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface ReservationService extends Service
{
    /**
     * @param request {@link AvailabilityCheckRequest}
     * @return {@link Boolean#TRUE} when given {@code request} is available,
     *         otherwise {@link String} report describing the reason why it is not available
     */
    @API
    public Object checkAvailability(AvailabilityCheckRequest request);

    /**
     * Creates a new reservation request.
     * <p/>
     * The user with the given {@code token} will be the request owner.
     *
     * @param token              token of the user requesting the operation
     * @param reservationRequest reservation request; should contains all required attributes
     * @return the created reservation request shongo-id
     */
    @API
    public String createReservationRequest(SecurityToken token, AbstractReservationRequest reservationRequest);

    /**
     * Modifies a given reservation request (by creating a new reservation request which is a modification of the given).
     *
     * @param token              token of the user requesting the operation
     * @param reservationRequest reservation request to be modified
     * @return the shongo-id of new reservation request which represents a modification of given reservation request
     */
    @API
    public String modifyReservationRequest(SecurityToken token, AbstractReservationRequest reservationRequest);

    /**
     * Revert a given modification of a reservation request. The reverting is allowed only when the reservation request
     * is not in {@link AllocationState#ALLOCATED}.
     *
     * @param token                token of the user requesting the operation
     * @param reservationRequestId shongo-id of the reservation request to be reverted
     * @return identifier of the reservation request that was modified by the reverted one
     */
    @API
    public String revertReservationRequest(SecurityToken token, String reservationRequestId);

    /**
     * Deletes a given reservation request.
     *
     * @param token                token of the user requesting the operation
     * @param reservationRequestId shongo-id of the reservation request to delete
     */
    @API
    public void deleteReservationRequest(SecurityToken token, String reservationRequestId);

    /**
     * Try to allocate reservation for given {@link ReservationRequest} (e.g., if it is in allocation failed state).
     *
     * @param token                token of the user requesting the operation
     * @param reservationRequestId shongo-id of the {@link ReservationRequest}
     */
    @API
    public void updateReservationRequest(SecurityToken token, String reservationRequestId);

    /**
     * List reservation requests which is the requesting user entitled to see.
     *
     * @param request {@link ReservationRequestListRequest}
     * @return {@link ListResponse} of {@link ReservationRequestSummary}s
     */
    @API
    public ListResponse<ReservationRequestSummary> listReservationRequests(ReservationRequestListRequest request);

    /**
     * Gets the complete Reservation object.
     *
     * @param token                token of the user requesting the operation
     * @param reservationRequestId shongo-id of the reservation request to get
     */
    @API
    public AbstractReservationRequest getReservationRequest(SecurityToken token, String reservationRequestId);

    /**
     * List child reservation requests for reservation request specified in the {@code request}.
     *
     * @param request {@link ChildReservationRequestListRequest}
     * @return {@link ListResponse} of {@link ReservationRequest}s
     */
    @API
    public ListResponse<ReservationRequest> listChildReservationRequests(ChildReservationRequestListRequest request);

    /**
     * @param token
     * @param reservationId
     * @return reservation with given shongo-id
     */
    @API
    public Reservation getReservation(SecurityToken token, String reservationId);

    /**
     * @param request {@link ReservationListRequest}
     * @return collection of already allocated {@link Reservation}s
     */
    @API
    public ListResponse<Reservation> listReservations(ReservationListRequest request);
}
