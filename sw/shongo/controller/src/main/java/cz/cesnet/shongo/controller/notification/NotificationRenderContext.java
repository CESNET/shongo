package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.authorization.Authorization;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.PeriodFormat;

import java.util.ResourceBundle;

/**
 * {@link Notification} context for rendering.
 */
public class NotificationRenderContext
{
    /**
     * @see NotificationConfiguration
     */
    private NotificationConfiguration notificationConfiguration;

    /**
     * Messages.
     */
    private ResourceBundle resourceBundle;

    /**
     * Constructor.
     *
     * @param notificationConfiguration sets the {@link #notificationConfiguration}
     */
    public NotificationRenderContext(NotificationConfiguration notificationConfiguration)
    {
        this.notificationConfiguration = notificationConfiguration;
    }

    /**
     * @param messagesFileName to load messages from
     */
    public void setMessages(String messagesFileName)
    {
        resourceBundle = ResourceBundle.getBundle(messagesFileName, notificationConfiguration.getLocale());
    }

    /**
     * @return {@link #notificationConfiguration#getTimeZone()}
     */
    public DateTimeZone getTimeZone()
    {
        return notificationConfiguration.getTimeZone();
    }

    /**
     * @param code
     * @return message for given {@code code}
     */
    public String message(String code)
    {
        if (resourceBundle == null) {
            return null;
        }
        String message = resourceBundle.getString(code);
        return message;
    }

    /**
     * @param timeZone to be formatted
     * @return {@code timeZone} formatted to string
     */
    public String formatTimeZone(DateTimeZone timeZone, DateTime dateTime)
    {
        if (timeZone.equals(DateTimeZone.UTC)) {
            return "UTC";
        }
        else {
            return DateTimeFormat.forPattern("ZZ")
                    .withZone(notificationConfiguration.getTimeZone())
                    .print(dateTime);
        }
    }

    /**
     * @param dateTime   to be formatted as UTC date/time
     * @param timeZone to be used
     * @return {@code dateTime} formatted to string
     */
    public String formatDateTime(DateTime dateTime, DateTimeZone timeZone)
    {
        if (dateTime.equals(Temporal.DATETIME_INFINITY_START) || dateTime.equals(Temporal.DATETIME_INFINITY_END)) {
            return "(infinity)";
        }
        else {
            String dateTimeString = DateTimeFormat.forPattern("d.M.yyyy HH:mm").withZone(timeZone).print(dateTime);
            return String.format("%s (%s)", dateTimeString, formatTimeZone(timeZone, dateTime));
        }
    }

    /**
     * @param dateTime   to be formatted as UTC date/time
     * @param timeZoneId to be used
     * @return {@code dateTime} formatted to string
     */
    public String formatDateTime(DateTime dateTime, String timeZoneId)
    {
        DateTimeZone dateTimeZone = DateTimeZone.forID(timeZoneId);
        return formatDateTime(dateTime, dateTimeZone);
    }

    /**
     * @param interval   whose start to be formatted as UTC date/time
     * @param timeZoneId to be used
     * @return {@code interval} start formatted to string
     */
    public String formatDateTime(Interval interval, String timeZoneId)
    {
        return formatDateTime(interval.getStart(), timeZoneId);
    }

    /**
     * @param dateTime to be formatted
     * @return {@code dateTime} formatted to string
     */
    public String formatDateTime(DateTime dateTime)
    {
        return formatDateTime(dateTime, notificationConfiguration.getTimeZone());
    }

    /**
     * @param duration to be formatted
     * @return {@code duration} formatted to string
     */
    public String formatDuration(Period duration)
    {
        if (duration.equals(Temporal.PERIOD_INFINITY)) {
            return "(infinity)";
        }
        return PeriodFormat.getDefault().print(Temporal.roundPeriod(duration));
    }

    /**
     * @param interval whose duration to be formatted
     * @return {@code interval} duration formatted to string
     */
    public String formatDuration(Interval interval)
    {
        return formatDuration(interval.toPeriod());
    }

    /**
     * @param userId user-id
     * @return {@link cz.cesnet.shongo.controller.common.Person} for given {@code userId}
     */
    public cz.cesnet.shongo.controller.api.Person getUserPerson(String userId)
    {
        return Authorization.getInstance().getUserPerson(userId).toApi();
    }

    /**
     * @param userInformation to be formatted
     * @return {@link cz.cesnet.shongo.api.UserInformation} formatted to string
     */
    public String formatUser(UserInformation userInformation)
    {
        return PersonInformation.Formatter.format(userInformation);
    }

    /**
     * @param userId to be formatted by it's {@link cz.cesnet.shongo.api.UserInformation}
     * @return {@link cz.cesnet.shongo.api.UserInformation} formatted to string
     */
    public String formatUser(String userId)
    {
        try {
            UserInformation userInformation = Authorization.getInstance().getUserInformation(userId);
            return PersonInformation.Formatter.format(userInformation);
        } catch (ControllerReportSet.UserNotExistException exception) {
            Notification.logger.warn("User '{}' doesn't exist.", userId);
            return "<not-exist> (" + userId + ")";
        }
    }
}
