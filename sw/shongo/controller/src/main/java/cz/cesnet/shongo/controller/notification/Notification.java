package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.authorization.Authorization;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.PeriodFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.*;

/**
 * Represents a notification.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class Notification
{
    protected static Logger logger = LoggerFactory.getLogger(NotificationManager.class);

    /**
     * Notification recipients. Each group should be notified separately.
     */
    private Map<RecipientGroup, Set<PersonInformation>> recipientsByGroup =
            new HashMap<RecipientGroup, Set<PersonInformation>>();

    /**
     * @return {@link #recipientsByGroup}
     */
    public Map<RecipientGroup, Set<PersonInformation>> getRecipientsByGroup()
    {
        return Collections.unmodifiableMap(recipientsByGroup);
    }

    /**
     * @return all recipients from {@link #recipientsByGroup}
     */
    public Collection<PersonInformation> getRecipients()
    {
        List<PersonInformation> recipients = new LinkedList<PersonInformation>();
        for (Set<PersonInformation> recipientsInGroup : recipientsByGroup.values()) {
            recipients.addAll(recipientsInGroup);
        }
        return recipients;
    }

    /**
     * Remove all added {@link #recipientsByGroup}.
     */
    public void clearRecipients()
    {
        recipientsByGroup.clear();
    }

    /**
     * @param recipient to be added to the {@link #recipientsByGroup}
     */
    public void addRecipient(RecipientGroup recipientGroup, PersonInformation recipient)
    {
        Set<PersonInformation> recipients = recipientsByGroup.get(recipientGroup);
        if (recipients == null) {
            recipients = new HashSet<PersonInformation>();
            recipientsByGroup.put(recipientGroup, recipients);
        }
        recipients.add(recipient);
    }

    /**
     * @param recipients to be added to the {@link #recipientsByGroup}
     */
    public void addRecipients(RecipientGroup recipientGroup, Collection<PersonInformation> recipients)
    {
        for (PersonInformation recipient : recipients) {
            addRecipient(recipientGroup, recipient);
        }
    }

    /**
     * @param userId for user to be added to the {@link #recipientsByGroup}
     */
    public void addUserRecipient(String userId)
    {
        try {
            UserInformation userRecipient = Authorization.getInstance().getUserInformation(userId);
            addRecipient(RecipientGroup.USER, userRecipient);
        } catch (ControllerReportSet.UserNotExistException exception) {
            logger.error("User '{}' doesn't exist.", userId);
        }
    }

    /**
     * @return string name of the {@link Notification}
     */
    public abstract String getName();

    /**
     * @return string content of the {@link Notification}
     */
    public abstract String getContent();

    /**
     * @return instance of {@link TemplateHelper} which will be added as "template" to velocity
     */
    public TemplateHelper getTemplateHelper()
    {
        return new TemplateHelper();
    }

    /**
     * Render given {@code notificationTemplateFileName} with specified {@code parameters}.
     *
     * @param notificationTemplateFileName to be rendered
     * @param parameters                   to be rendered
     * @return rendered string
     */
    public String renderTemplate(String notificationTemplateFileName, Map<String, Object> parameters)
    {
        try {
            Template template = getConfiguration().getTemplate("notification/" + notificationTemplateFileName);

            StringWriter stringWriter = new StringWriter();
            parameters.put("template", getTemplateHelper());
            template.process(parameters, stringWriter);
            return stringWriter.toString();
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Single instance of {@link Configuration}.
     */
    private static Configuration configuration;

    /**
     * @return {@link #configuration}
     */
    private static Configuration getConfiguration()
    {
        if (configuration == null) {
            configuration = new Configuration();
            configuration.setObjectWrapper(new DefaultObjectWrapper());
            configuration.setClassForTemplateLoading(Notification.class, "/");
        }
        return configuration;
    }

    /**
     * Helper containing common functions which can be used in templates (by "$template" variable).
     */
    public class TemplateHelper
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
         * @return {@link UserInformation} formatted to string
         */
        public String formatUser(UserInformation userInformation)
        {
            return PersonInformation.Formatter.format(userInformation);
        }

        /**
         * @param userId to be formatted by it's {@link UserInformation}
         * @return {@link UserInformation} formatted to string
         */
        public String formatUser(String userId)
        {
            try {
                UserInformation userInformation = Authorization.getInstance().getUserInformation(userId);
                return PersonInformation.Formatter.format(userInformation);
            } catch (ControllerReportSet.UserNotExistException exception) {
                logger.warn("User '{}' doesn't exist.", userId);
                return "<not-exist> (" + userId + ")";
            }
        }
    }

    /**
     * Enumeration of recipient groups. Each group is notified separately.
     */
    public enum RecipientGroup
    {
        /**
         * Group of Shongo users.
         */
        USER,

        /**
         * Group of Shongo administrators.
         */
        ADMINISTRATOR
    }
}
