package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;

import java.util.*;

/**
 * Set of {@link AbstractNotification}s for execution.
 */
public class NotificationBuilder
{
    /**
     * @see ReservationRequestManager
     */
    private ReservationRequestManager reservationRequestManager;

    /**
     * @see AuthorizationManager
     */
    private AuthorizationManager authorizationManager;

    /**
     * @see ControllerConfiguration
     */
    private ControllerConfiguration configuration;

    /**
     * List of {@link AbstractNotification}s.
     */
    private List<AbstractNotification> notifications = new LinkedList<AbstractNotification>();

    /**
     * Map of {@link ReservationRequestNotification} by {@link AbstractReservationRequest}.
     */
    private Map<Long, ReservationRequestNotification> reservationRequestNotifications =
            new HashMap<Long, ReservationRequestNotification>();

    /**
     * Constructor.
     *
     * @param authorizationManager sets the {@link #authorizationManager}
     * @param configuration        sets the {@link #configuration}
     */
    public NotificationBuilder(AuthorizationManager authorizationManager, ControllerConfiguration configuration)
    {
        this.reservationRequestManager = new ReservationRequestManager(authorizationManager.getEntityManager());
        this.authorizationManager = authorizationManager;
        this.configuration = configuration;
    }

    /**
     * @return {@link #notifications}
     */
    public List<AbstractNotification> getNotifications()
    {
        return Collections.unmodifiableList(notifications);
    }

    /**
     * @param notification to be added to the {@link NotificationBuilder}
     */
    public void addNotification(AbstractNotification notification)
    {
        notifications.add(notification);

        if (notification instanceof AbstractReservationRequestNotification) {
            AbstractReservationRequestNotification abstractReservationRequestNotification =
                    (AbstractReservationRequestNotification) notification;
            Long reservationRequestId = ObjectIdentifier.parseId(AbstractReservationRequest.class,
                    abstractReservationRequestNotification.getReservationRequestId());
            if (reservationRequestId != null) {
                // Get top reservation request
                AbstractReservationRequest abstractReservationRequest =
                        reservationRequestManager.get(reservationRequestId);
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
                    reservationRequestNotification =
                            new ReservationRequestNotification(abstractReservationRequest, authorizationManager);
                    notifications.add(reservationRequestNotification);
                    reservationRequestNotifications.put(abstractReservationRequestId, reservationRequestNotification);
                }

                // Add reservation notification to reservation request notification
                reservationRequestNotification.addNotification(notification);
            }
        }
    }
}
