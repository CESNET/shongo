package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.controller.util.iCalendar;

import javax.persistence.EntityManager;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class iCalendarNotificationAttachment extends NotificationAttachment
{
    /**
     * @see cz.cesnet.shongo.controller.util.iCalendar
     */
    private final iCalendar calendar;

    private NotificationState notificationState;

    /**
     * Constructor.
     *
     * @param fileName sets the {@link #fileName}
     *                 @param calendar sets the {@link #calendar}
     */
    public iCalendarNotificationAttachment(String fileName, iCalendar calendar, NotificationState notificationState)
    {
        super(fileName);
        if (calendar == null) {
            throw new IllegalArgumentException(iCalendar.class.getSimpleName() + " must be not null.");
        }
        this.calendar = calendar;
        this.notificationState = notificationState;
    }

    /**
     * Constructor.
     *
     * @param fileName sets the {@link #fileName}
     * @param calendar sets the {@link #calendar}
     */
    public iCalendarNotificationAttachment(String fileName, iCalendar calendar)
    {
        this(fileName, calendar, null);
    }

    /**
     * @param organizer
     * @return file content
     */
    public String getFileContent(EntityManager entityManager)
    {
        if (notificationState != null) {
            calendar.setSequence(NotificationState.getSequence(notificationState, entityManager));
        }
        return calendar.toString();
    }
}
