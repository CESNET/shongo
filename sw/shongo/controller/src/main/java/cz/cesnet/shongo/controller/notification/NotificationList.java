package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Set of {@link AbstractNotification}s for execution.
 */
public class NotificationList extends LinkedList<AbstractNotification>
{
    /**
     * @see ControllerConfiguration
     */
    private ControllerConfiguration configuration;

    /**
     * Map of {@link ReservationRequestNotification} by {@link cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest}.
     */
    private Map<Long, ReservationRequestNotification> reservationRequestNotifications =
            new HashMap<Long, ReservationRequestNotification>();

    /**
     * Constructor.
     *
     * @param configuration sets the {@link #configuration}
     */
    public NotificationList(ControllerConfiguration configuration)
    {
        this.configuration = configuration;
    }

    /**
     * @param notification to be added to the {@link NotificationList}
     */
    public void addNotification(AbstractNotification notification)
    {
        add(notification);
    }

    /**
     * Add new {@link ReservationNotification} to the {@link NotificationList}.
     *
     * @param reservation
     * @param type
     * @param authorizationManager
     */
    public void addReservationEvent(Reservation reservation, ReservationNotification.Type type,
            AuthorizationManager authorizationManager)
    {
        // Get reservation request for reservation
        Allocation allocation = reservation.getAllocation();
        AbstractReservationRequest reservationRequest =
                (allocation != null ? allocation.getReservationRequest() : null);

        // Create reservation notification
        ReservationNotification notification = new ReservationNotification(
                type, reservation, reservationRequest, authorizationManager, configuration);

        // Get reservation request notification
        if (reservationRequest != null) {
            // Add reservation notification as normal and add it also to reservation request notification
            addReservationRequestEvent(notification, reservationRequest, authorizationManager);
        }
        else {
            // Add reservation notification as normal
            addNotification(notification);
        }
    }

    /**
     * Add new {@link ReservationRequestNotification} to the {@link NotificationList}.
     *
     * @param reservation
     * @param type
     * @param authorizationManager
     */
    public void addReservationRequestEvent(AbstractNotification notification,
            AbstractReservationRequest abstractReservationRequest, AuthorizationManager authorizationManager)
    {
        addNotification(notification);

        // Get top reservation request
        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;
            Allocation parentAllocation = reservationRequest.getParentAllocation();
            if (parentAllocation != null) {
                AbstractReservationRequest parentReservationRequest = parentAllocation.getReservationRequest();
                if (parentReservationRequest != null) {
                    abstractReservationRequest = parentReservationRequest;
                }
            }
        }

        // Create or reuse reservation request notification
        Long abstractReservationRequestId = abstractReservationRequest.getId();
        ReservationRequestNotification reservationRequestNotification =
                reservationRequestNotifications.get(abstractReservationRequestId);
        if (reservationRequestNotification == null) {
            reservationRequestNotification = new ReservationRequestNotification(
                    abstractReservationRequest, authorizationManager, configuration);
            add(reservationRequestNotification);
            reservationRequestNotifications.put(abstractReservationRequestId, reservationRequestNotification);
        }

        // Add reservation notification to reservation request notification
        reservationRequestNotification.addEvent(notification);
    }
}
