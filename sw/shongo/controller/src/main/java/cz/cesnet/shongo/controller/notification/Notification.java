package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.controller.common.Person;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.PeriodFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
    private List<Person> recipients = new ArrayList<Person>();

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
    public List<Person> getRecipients()
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
     * @param parameters to be rendered
     * @return rendered string
     */
    public String renderTemplate(String notificationTemplateFileName, Map<String, Object> parameters)
    {
        VelocityEngine velocityEngine = getVelocityEngine();
        Template template = velocityEngine.getTemplate("notification/" + notificationTemplateFileName);
        VelocityContext context = new VelocityContext();
        context.put("template", getTemplateHelper());
        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                context.put(entry.getKey(), entry.getValue());
            }
        }

        StringWriter stringWriter = new StringWriter();
        template.merge(context, stringWriter);
        return stringWriter.toString();
    }

    /**
     * Single instance of {@link VelocityEngine}.
     */
    private static VelocityEngine velocityEngine;

    /**
     * @return {@link #velocityEngine}
     */
    private static VelocityEngine getVelocityEngine()
    {
        if (velocityEngine == null) {
            Properties properties = new Properties();
            properties.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
            properties.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
            velocityEngine = new VelocityEngine();
            velocityEngine.init(properties);
        }
        return velocityEngine;
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
            return DateTimeFormat.forPattern("d.M.yyyy HH:mm").print(dateTime);
        }

        /**
         * @param dateTime to be formatted as UTC date/time
         * @return {@code dateTime} formatted to string
         */
        public String formatDateTimeUTC(DateTime dateTime)
        {
            return DateTimeFormat.forPattern("d.M.yyyy HH:mm").print(dateTime.withZone(DateTimeZone.UTC));
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
         * @param interval whose start to be formatted as UTC date/time
         * @return {@code interval} start formatted to string
         */
        public String formatDateTimeUTC(Interval interval)
        {
            return formatDateTimeUTC(interval.getStart());
        }

        /**
         * @param duration to be formatted
         * @return {@code duration} formatted to string
         */
        public String formatDuration(Period duration)
        {
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
         * @return {@link Person} for given {@code userId}
         */
        public cz.cesnet.shongo.controller.api.Person getUserPerson(String userId)
        {
            return notificationManager.getAuthorization().getUserPerson(userId).toApi();
        }

        /**
         * @param person to be formatted
         * @return {@link Person} formatted to string
         */
        public String formatPerson(cz.cesnet.shongo.controller.api.Person person)
        {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(person.getName());
            if (person.getOrganization() != null) {
                stringBuilder.append(", ");
                stringBuilder.append(person.getOrganization());
            }
            return stringBuilder.toString();
        }

        /**
         * @param userId to be formatted by it's {@link Person}
         * @return {@link Person} formatted to string
         */
        public String formatUser(String userId)
        {
            return formatPerson(getUserPerson(userId));
        }
    }
}
