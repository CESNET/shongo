package cz.cesnet.shongo.controller.notification;

import org.joda.time.DateTimeZone;

import java.util.Locale;

/**
 * Represents a configuration for rendering of a {@link Notification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NotificationConfiguration
{
    /**
     * Specifies locale (e.g., for language).
     */
    private Locale locale;

    /**
     * Specifies timezone for date/times.
     */
    private DateTimeZone dateTimeZone;

    /**
     * Specifies whether notification should contain administrator information.
     */
    boolean administrator;
}
