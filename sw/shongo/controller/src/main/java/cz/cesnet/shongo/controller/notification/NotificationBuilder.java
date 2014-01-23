package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;

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
     * List of {@link AbstractNotification}s.
     */
    private List<AbstractNotification> notifications = new LinkedList<AbstractNotification>();

    /**
     * Constructor.
     *
     * @param authorizationManager sets the {@link #authorizationManager}
     */
    public NotificationBuilder(AuthorizationManager authorizationManager)
    {
        this.reservationRequestManager = new ReservationRequestManager(authorizationManager.getEntityManager());
        this.authorizationManager = authorizationManager;
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
            onAddNotification((AbstractReservationRequestNotification) notification);
        }
        else if (notification instanceof RoomParticipationNotification.Simple) {
            onAddNotification((RoomParticipationNotification.Simple) notification);
        }
    }

    /**
     *
     * @param notifications to be added to the {@link NotificationBuilder}
     */
    public void addNotifications(Collection<AbstractNotification> notifications)
    {
        for (AbstractNotification notification : notifications) {
            addNotification(notification);
        }
    }

    /**
     * Map of {@link ReservationRequestNotification} by {@link AbstractReservationRequest}.
     */
    private Map<Long, ReservationRequestNotification> reservationRequestNotifications =
            new HashMap<Long, ReservationRequestNotification>();

    /**
     * @param abstractReservationRequestNotification to be initialized
     */
    private void onAddNotification(AbstractReservationRequestNotification abstractReservationRequestNotification)
    {
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
            reservationRequestNotification.addNotification(abstractReservationRequestNotification);
        }
    }

    /**
     * Map of {@link ReservationRequestNotification} by {@link AbstractReservationRequest}.
     */
    private Map<Long, RoomParticipationNotification> roomParticipationNotifications =
            new HashMap<Long, RoomParticipationNotification>();

    /**
     * @param notification to be initialized
     */
    private void onAddNotification(RoomParticipationNotification.Simple notification)
    {
        Long reservationRequestId = notification.getReservationRequestId();
        if (reservationRequestId == null) {
            return;
        }
        RoomParticipationNotification existingNotification =
                roomParticipationNotifications.get(reservationRequestId);
        if (existingNotification != null) {
            RoomParticipationNotification.Deleted deletedNotification = null;
            RoomParticipationNotification.Created createdNotification = null;
            if (existingNotification instanceof RoomParticipationNotification.Deleted) {
                deletedNotification = (RoomParticipationNotification.Deleted) existingNotification;
            }
            else if (existingNotification instanceof RoomParticipationNotification.Created) {
                createdNotification = (RoomParticipationNotification.Created) existingNotification;
            }
            if (notification instanceof RoomParticipationNotification.Deleted) {
                deletedNotification = (RoomParticipationNotification.Deleted) notification;
            }
            else if (notification instanceof RoomParticipationNotification.Created) {
                createdNotification = (RoomParticipationNotification.Created) notification;
            }
            if (createdNotification != null && deletedNotification != null) {
                RoomParticipationNotification modifiedNotification = new RoomParticipationNotification.Modified(
                        deletedNotification, createdNotification, authorizationManager);
                if (modifiedNotification.hasRecipients()) {
                    addNotification(modifiedNotification);
                }
            }
        }
        else {
            roomParticipationNotifications.put(reservationRequestId, notification);
        }
    }
}
