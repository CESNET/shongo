package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * {@link ConfigurableNotification} for changes in allocation of {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestNotification extends AbstractReservationRequestNotification
{
    private Target target;

    /**
     * List of {@link AbstractNotification}s which are part of the {@link ReservationNotification}.
     */
    private List<AbstractReservationRequestNotification> notifications =
            new LinkedList<AbstractReservationRequestNotification>();

    /**
     * Constructor.
     *
     * @param reservationRequest
     * @param authorizationManager
     */
    public ReservationRequestNotification(AbstractReservationRequest reservationRequest,
            AuthorizationManager authorizationManager)
    {
        super(reservationRequest);

        EntityManager entityManager = authorizationManager.getEntityManager();

        this.target = Target.createInstance(reservationRequest, entityManager);

        for (String userId : authorizationManager.getUserIdsWithRole(reservationRequest, ObjectRole.OWNER)) {
            addRecipient(authorizationManager.getUserInformation(userId), false);
        }
    }

    /**
     * @param notification to be added to the {@link #notifications}
     */
    public void addNotification(AbstractReservationRequestNotification notification)
    {
        notifications.add(notification);
        Collections.sort(notifications, new Comparator<AbstractReservationRequestNotification>()
        {
            @Override
            public int compare(AbstractReservationRequestNotification notification1,
                    AbstractReservationRequestNotification notification2)
            {
                return notification1.getSlotStart().compareTo(notification2.getSlotStart());
            }
        });
    }

    @Override
    protected Collection<Locale> getAvailableLocals()
    {
        return NotificationMessage.AVAILABLE_LOCALES;
    }

    @Override
    public Interval getSlot()
    {
        throw new TodoImplementException();
    }

    @Override
    protected ConfigurableNotification.Configuration createConfiguration(Locale locale, DateTimeZone timeZone,
            boolean administrator)
    {
        return new ParentConfiguration(locale, timeZone, administrator);
    }

    @Override
    protected NotificationMessage renderMessage(Configuration configuration, NotificationManager manager)
    {
        RenderContext renderContext = new ConfiguredRenderContext(configuration, "notification", manager);

        // Number of child events of each type
        int allocationFailedNotifications = 0;
        int newReservationNotifications = 0;
        int deletedReservationNotifications = 0;
        for (AbstractNotification notification : notifications) {
            if (notification instanceof AllocationFailedNotification) {
                allocationFailedNotifications++;
            }
            else if (notification instanceof ReservationNotification.New) {
                newReservationNotifications++;
            }
            else if (notification instanceof ReservationNotification.Deleted) {
                deletedReservationNotifications++;
            }
            else {
                throw new TodoImplementException(notification.getClass());
            }
        }

        // Description of reservation request target
        StringBuilder targetBuilder = new StringBuilder();
        targetBuilder.append(renderContext.message("reservationRequest.for." + target.getType()));
        if (target instanceof Target.Room) {
            Target.Room room = (Target.Room) target;
            String roomName = room.getName();
            if (roomName != null) {
                targetBuilder.append(" ");
                targetBuilder.append(roomName);
            }
        }
        else if (target instanceof Target.Alias) {
            Target.Alias alias = (Target.Alias) target;
            String roomName = alias.getRoomName();
            if (roomName != null) {
                targetBuilder.append(" ");
                targetBuilder.append(roomName);
            }
        }

        // Description of the reservation request result
        StringBuilder resultDescriptionBuilder = new StringBuilder();
        int totalNotifications = notifications.size();
        if (totalNotifications == allocationFailedNotifications) {
            resultDescriptionBuilder.append(renderContext.message("reservationRequest.result.failed"));
        }
        else if (totalNotifications == newReservationNotifications
                || totalNotifications == deletedReservationNotifications) {
            resultDescriptionBuilder.append(renderContext.message("reservationRequest.result.success"));
        }
        else {
            Map<String, Integer> childrenTypes = new HashMap<String, Integer>();
            childrenTypes.put("reservationRequest.child.failed", allocationFailedNotifications);
            childrenTypes.put("reservationRequest.child.new", newReservationNotifications);
            childrenTypes.put("reservationRequest.child.deleted", deletedReservationNotifications);

            if (allocationFailedNotifications > 0) {
                resultDescriptionBuilder.append(renderContext.message("reservationRequest.result.partialSuccess"));
            }
            else {
                resultDescriptionBuilder.append(renderContext.message("reservationRequest.result.success"));
            }
            resultDescriptionBuilder.append(" (");
            resultDescriptionBuilder.append(renderContext.message("reservationRequest.child"));
            resultDescriptionBuilder.append(": ");
            boolean separator = false;
            for (Map.Entry<String, Integer> entry : childrenTypes.entrySet()) {
                if (entry.getValue() == 0) {
                    continue;
                }
                if (separator) {
                    resultDescriptionBuilder.append(", ");
                }
                resultDescriptionBuilder.append(renderContext.message(
                        entry.getKey(), entry.getValue()));
                separator = true;
            }
            resultDescriptionBuilder.append(")");
        }

        // Build notification title
        StringBuilder titleBuilder = new StringBuilder();
        titleBuilder.append(renderContext.message(
                "reservationRequest", renderContext.formatDateTime(getReservationRequestUpdatedAt()), targetBuilder));
        titleBuilder.append(" ");
        titleBuilder.append(resultDescriptionBuilder);

        NotificationMessage message = renderTemplateMessage(
                renderContext, titleBuilder.toString(), "reservation-request.ftl");
        for (AbstractNotification notification : notifications) {
            NotificationMessage childMessage;
            if (notification instanceof ConfigurableNotification) {
                ConfigurableNotification configurableEvent = (ConfigurableNotification) notification;
                childMessage = configurableEvent.renderMessage(configuration, manager);
            }
            else {
                throw new TodoImplementException(notification.getClass());
            }
            message.appendChildMessage(childMessage);
        }
        return message;
    }

    @Override
    protected void onAfterAdded(NotificationManager notificationManager, EntityManager entityManager)
    {
        super.onAfterAdded(notificationManager, entityManager);

        Long reservationRequestId = ObjectIdentifier.parseId(
                AbstractReservationRequest.class, getReservationRequestId());
        notificationManager.reservationRequestNotificationsById.put(reservationRequestId, this);
    }

    @Override
    protected void onAfterRemoved(NotificationManager notificationManager)
    {
        super.onAfterRemoved(notificationManager);

        Long reservationRequestId = ObjectIdentifier.parseId(
                AbstractReservationRequest.class, getReservationRequestId());
        notificationManager.reservationRequestNotificationsById.remove(reservationRequestId);
    }
}
