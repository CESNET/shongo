package cz.cesnet.shongo.controller.notification;

import org.joda.time.DateTimeZone;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Represents a configuration for rendering of a {@link Notification} (each {@link Notification} can be rendered
 * for one or more {@link NotificationConfiguration}s).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NotificationConfiguration
{
    /**
     * Specifies locale (e.g., for language).
     */
    private final Locale locale;

    /**
     * Specifies timezone for date/times.
     */
    private final DateTimeZone timeZone;

    /**
     * Specifies whether notification should contain administrator information.
     */
    private final boolean administrator;

    /**
     * Constructor.
     *
     * @param locale sets the {@link #locale}
     * @param timeZone sets the {@link #timeZone}
     * @param administrator sets the {@link #administrator}
     */
    public NotificationConfiguration(Locale locale, DateTimeZone timeZone, boolean administrator)
    {
        this.locale = locale;
        this.timeZone = timeZone;
        this.administrator = administrator;
    }

    /**
     * @return {@link #locale}
     */
    public Locale getLocale()
    {
        return locale;
    }

    /**
     * @return {@link #timeZone}
     */
    public DateTimeZone getTimeZone()
    {
        return timeZone;
    }

    /**
     * @return {@link #administrator}
     */
    public boolean isAdministrator()
    {
        return administrator;
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        NotificationConfiguration notificationConfiguration = (NotificationConfiguration) object;
        if (administrator != notificationConfiguration.administrator) {
            return false;
        }
        if (!locale.equals(notificationConfiguration.locale)) {
            return false;
        }
        if (!timeZone.equals(notificationConfiguration.timeZone)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = locale.hashCode();
        result = 31 * result + timeZone.hashCode();
        result = 31 * result + (administrator ? 1 : 0);
        return result;
    }
}
