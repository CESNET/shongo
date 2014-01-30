package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * {@link ConfigurableNotification}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomGroupNotification extends ConfigurableNotification
{
    private RoomEndpoint roomEndpoint;

    /**
     * List of {@link AbstractNotification}s which are part of the {@link ReservationNotification}.
     */
    private Map<RoomNotification, Set<PersonInformation>> notifications =
            new HashMap<RoomNotification, Set<PersonInformation>>();

    public RoomGroupNotification(RoomEndpoint roomEndpoint)
    {
        this.roomEndpoint = roomEndpoint;
    }

    public void addNotification(RoomNotification notification)
    {
        Set<PersonInformation> recipients = new HashSet<PersonInformation>(notification.getRecipients());
        addRecipients(recipients);
        notifications.put(notification, recipients);
        notification.clearRecipients();
    }

    @Override
    protected Collection<Locale> getAvailableLocals()
    {
        return NotificationMessage.AVAILABLE_LOCALES;
    }

    @Override
    protected NotificationMessage getRenderedMessage(PersonInformation recipient,
            Configuration configuration, NotificationManager manager)
    {
        RenderContext renderContext = new ConfiguredRenderContext(configuration, "notification", manager);

        // Build notification title
        StringBuilder titleBuilder = new StringBuilder();
        titleBuilder.append("RoomGroupNotification for " + roomEndpoint.getId());

        NotificationMessage message = new NotificationMessage(
                renderContext.getLanguage(), titleBuilder.toString(), "content");
        for (AbstractNotification notification : notifications.keySet()) {
            Set<PersonInformation> recipients = notifications.get(notification);
            if (!recipients.contains(recipient)) {
                continue;
            }
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
    protected NotificationMessage renderMessage(Configuration configuration,
            NotificationManager manager)
    {
        throw new TodoImplementException();
    }

    @Override
    protected void onAfterAdded(NotificationManager notificationManager, EntityManager entityManager)
    {
        super.onAfterAdded(notificationManager, entityManager);

        notificationManager.roomGroupNotificationByRoomEndpointId.put(roomEndpoint.getId(), this);
    }

    @Override
    protected void onAfterRemoved(NotificationManager notificationManager, EntityManager entityManager)
    {
        super.onAfterRemoved(notificationManager, entityManager);

        notificationManager.roomGroupNotificationByRoomEndpointId.remove(roomEndpoint.getId());
    }
}
