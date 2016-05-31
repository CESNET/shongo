package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.notification.executor.NotificationExecutor;

import javax.persistence.EntityManager;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link NotificationExecutor} for testing.
 */
public class TestingNotificationExecutor extends NotificationExecutor
{
    /**
     * Executed {@link AbstractNotification}.
     */
    private List<NotificationRecord> notificationRecords = new LinkedList<NotificationRecord>();

    /**
     * @return size of {@link #notificationRecords}
     */
    public int getNotificationCount()
    {
        return notificationRecords.size();
    }

    /**
     * @return {@link #notificationRecords}
     */
    public List<NotificationRecord> getNotificationRecords()
    {
        return notificationRecords;
    }

    @Override
    public void executeNotification(PersonInformation recipient, AbstractNotification notification,
                                    NotificationManager manager, EntityManager entityManager)
    {
        NotificationMessage recipientMessage = notification.getMessage(recipient, manager, entityManager);
        logger.debug("Notification for {} (reply-to: {})...\nSUBJECT:\n{}\n\nCONTENT:\n{}", new Object[]{
                recipient, notification.getReplyTo(), recipientMessage.getTitle(), recipientMessage.getContent()
        });
        for (NotificationAttachment attachment : recipientMessage.getAttachments()) {
            iCalendarNotificationAttachment calendarAttachment = (iCalendarNotificationAttachment) attachment;
            String fileContent = calendarAttachment.getFileContent("test", entityManager);
            logger.debug("ATTACHMENT {}:\n{}", attachment.getFileName(), fileContent);
        }
        notificationRecords.add(new NotificationRecord<AbstractNotification>(recipient, notification));
        if (notification instanceof RoomGroupNotification) {
            RoomGroupNotification roomGroupNotification = (RoomGroupNotification) notification;
            for (RoomNotification roomNotification : roomGroupNotification.getNotifications(recipient)) {
                notificationRecords.add(new NotificationRecord<AbstractNotification>(recipient, roomNotification));
            }
        }
    }

    public class NotificationRecord<T extends AbstractNotification>
    {
        private final PersonInformation recipient;

        private final T notification;

        private NotificationRecord(PersonInformation recipient, T notification)
        {
            this.recipient = recipient;
            this.notification = notification;
        }

        public PersonInformation getRecipient()
        {
            return recipient;
        }

        public T getNotification()
        {
            return notification;
        }
    }
}
