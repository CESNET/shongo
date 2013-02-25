package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.Authorization;
import cz.cesnet.shongo.controller.common.Person;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a notification.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class Notification
{
    protected static Logger logger = LoggerFactory.getLogger(NotificationManager.class);

    /**
     * @see NotificationManager
     */
    private NotificationManager notificationManager;

    /**
     * Notification recipients.
     */
    private Set<Person> recipients = new HashSet<Person>();

    /**
     * Constructor.
     *
     * @param notificationManager sets the {@link #notificationManager}
     */
    public Notification(NotificationManager notificationManager)
    {
        this.notificationManager = notificationManager;
    }

    /**
     * @return {@link #notificationManager}
     */
    public NotificationManager getNotificationManager()
    {
        return notificationManager;
    }

    /**
     * @return {@link #recipients}
     */
    public Collection<Person> getRecipients()
    {
        return recipients;
    }

    /**
     * @param recipient to be added to the {@link #recipients}
     */
    public void addRecipient(Person recipient)
    {
        recipients.add(recipient);
    }

    /**
     * @param userId for user to be added to the {@link #recipients}
     */
    public void addUserRecipient(String userId)
    {
        Person person = notificationManager.getAuthorization().getUserPerson(userId);
        addRecipient(person);
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
            throw new IllegalStateException(exception);
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
            if (dateTime.equals(Temporal.DATETIME_INFINITY_START) || dateTime.equals(Temporal.DATETIME_INFINITY_END)) {
                dateTimeString = "(infinity)";
                dateTime = DateTime.now();
            }
            else {
                dateTimeString = DateTimeFormat.forPattern("d.M.yyyy HH:mm").print(dateTime);
            }

            DateTimeZone dateTimeZone = DateTimeZone.forID(timeZoneId);
            dateTime = dateTime.withZone(dateTimeZone);
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
            return PeriodFormat.getDefault().print(duration);
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
            return notificationManager.getAuthorization().getUserPerson(userId).toApi();
        }

        /**
         * @param name
         * @param organization
         * @return {@link cz.cesnet.shongo.controller.common.Person} formatted to string
         */
        public String formatPerson(String name, String organization)
        {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(name);
            if (organization != null) {
                stringBuilder.append(", ");
                stringBuilder.append(organization);
            }
            return stringBuilder.toString();
        }

        /**
         * @param userId to be formatted by it's {@link cz.cesnet.shongo.controller.common.Person}
         * @return {@link cz.cesnet.shongo.controller.common.Person} formatted to string
         */
        public String formatUser(String userId)
        {
            Authorization.UserInformation userInformation = Authorization.UserInformation.getInstance(userId);
            return formatPerson(userInformation.getFullName(), userInformation.getRootOrganization());
        }
    }
}
