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

/**
 * Helper containing common functions which can be used in templates (by "$template" variable).
 */
public class NotificationTemplateHelper
{
    /**
     * @param dateTime to be formatted
     * @return {@code dateTime} formatted to string
     */
    public String formatDateTime(DateTime dateTime)
    {
        if (dateTime.equals(Temporal.DATETIME_INFINITY_START) || dateTime.equals(Temporal.DATETIME_INFINITY_END)) {
            return "(infinity)";
        }
        else {
            return DateTimeFormat.forPattern("d.M.yyyy HH:mm").print(dateTime);
        }
    }

    /**
     * @param dateTime   to be formatted as UTC date/time
     * @param timeZoneId to be used
     * @return {@code dateTime} formatted to string
     */
    public String formatDateTime(DateTime dateTime, String timeZoneId)
    {
        String dateTimeString = "";
        DateTimeZone dateTimeZone = DateTimeZone.forID(timeZoneId);
        if (dateTime.equals(Temporal.DATETIME_INFINITY_START) || dateTime.equals(Temporal.DATETIME_INFINITY_END)) {
            dateTimeString = "(infinity)";
            dateTime = DateTime.now(dateTimeZone);
        }
        else {
            dateTime = dateTime.withZone(dateTimeZone);
            dateTimeString = DateTimeFormat.forPattern("d.M.yyyy HH:mm").print(dateTime);
        }

        String dateTimeZoneOffset = "";
        if (!dateTimeZone.equals(DateTimeZone.UTC)) {
            int offset = dateTimeZone.getOffset(dateTime) / 60000;
            dateTimeZoneOffset = String.format(")(%+03d:%02d", offset / 60, Math.abs(offset % 60));
        }

        return String.format("%s (%s%s)", dateTimeString, timeZoneId, dateTimeZoneOffset);
    }

    /**
     * @param interval whose start to be formatted
     * @return {@code interval} start formatted to string
     */
    public String formatDateTime(Interval interval)
    {
        return formatDateTime(interval.getStart());
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
