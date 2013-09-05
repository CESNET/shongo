package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.common.MessageSource;
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
 * Represents an abstract {@link Notification} for a simple list of recipients. The {@link NotificationMessage} must
 * be rendered in {@link #renderMessageForRecipient} abstract method. You can use {@link #renderMessageFromTemplate} method for the
 * rendering based on templates.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractNotification implements Notification
{
    protected static Logger logger = LoggerFactory.getLogger(Notification.class);

    /**
     * List of recipient {@link PersonInformation}s.
     */
    private Set<PersonInformation> recipients = new HashSet<PersonInformation>();

    @Override
    public boolean addRecipient(PersonInformation recipient)
    {
        return recipients.add(recipient);
    }

    /**
     * @param recipients to be added by {@link #addRecipient}
     */
    public final void addRecipients(Collection<PersonInformation> recipients)
    {
        for (PersonInformation recipient : recipients) {
            addRecipient(recipient);
        }
    }

    @Override
    public void clearRecipients()
    {
        recipients.clear();
    }

    @Override
    public final Collection<PersonInformation> getRecipients()
    {
        return Collections.unmodifiableCollection(recipients);
    }

    @Override
    public final boolean hasRecipients()
    {
        return recipients.size() > 0;
    }

    @Override
    public final NotificationMessage getRecipientMessage(PersonInformation recipient)
    {
        return renderMessageForRecipient(recipient);
    }

    /**
     * Render {@link NotificationMessage} for given {@code recipient}.
     *
     * @param recipient for who the message should be rendered
     * @return rendered {@link NotificationMessage}
     */
    protected abstract NotificationMessage renderMessageForRecipient(PersonInformation recipient);

    /**
     * Render {@link NotificationMessage} from template with given {@code fileName}.
     *
     * @param renderContext to be used for rendering
     * @param title         message title
     * @param fileName      message template filename
     * @return rendered {@link NotificationMessage}
     */
    protected final NotificationMessage renderMessageFromTemplate(RenderContext renderContext, String title, String fileName)
    {
        Map<String, Object> templateParameters = new HashMap<String, Object>();
        templateParameters.put("context", renderContext);
        templateParameters.put("notification", this);
        for (Map.Entry<String, Object> parameter : renderContext.getParameters().entrySet()) {
            templateParameters.put(parameter.getKey(), parameter.getValue());
        }
        String content = renderTemplate(fileName, templateParameters);
        return new NotificationMessage(title, content);
    }

    @Override
    public final String toString()
    {
        return getClass().getSimpleName();
    }

    /**
     * Render given {@code templateFileName} with specified {@code templateParameters}.
     *
     * @param templateFileName   to be rendered
     * @param templateParameters to be rendered
     * @return rendered template as string
     */
    public static String renderTemplate(String templateFileName, Map<String, Object> templateParameters)
    {
        try {
            Template template = getTemplateConfiguration().getTemplate("notification/" + templateFileName);
            StringWriter stringWriter = new StringWriter();
            template.process(templateParameters, stringWriter);
            return stringWriter.toString();
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Single instance of {@link freemarker.template.Configuration}.
     */
    private static Configuration templateConfiguration;

    /**
     * @return {@link #templateConfiguration}
     */
    private static Configuration getTemplateConfiguration()
    {
        if (templateConfiguration == null) {
            templateConfiguration = new Configuration();
            templateConfiguration.setObjectWrapper(new DefaultObjectWrapper());
            templateConfiguration.setClassForTemplateLoading(AbstractNotification.class, "/");
        }
        return templateConfiguration;
    }

    /**
     * Context for rendering of {@link AbstractNotification}.
     */
    public static class RenderContext
    {
        /**
         * {@link MessageSource} for message which can be used for rendering.
         */
        private MessageSource messageSource;

        /**
         * Parameters.
         */
        private Map<String, Object> parameters = new HashMap<String, Object>();

        /**
         * Constructor.
         *
         * @param messageSource sets the {@link #messageSource}
         */
        public RenderContext(MessageSource messageSource)
        {
            this.messageSource = messageSource;
        }

        /**
         * @return {@link DateTimeZone} which should be used for date/time formatting
         */
        public DateTimeZone getTimeZone()
        {
            return DateTimeZone.getDefault();
        }

        /**
         * @return {@link #parameters}
         */
        public Map<String, Object> getParameters()
        {
            return parameters;
        }

        /**
         * @param name
         * @param value
         */
        public void addParameter(String name, Object value)
        {
            parameters.put(name, value);
        }

        /**
         * @param code
         * @return message for given {@code code}
         */
        public String message(String code)
        {
            if (messageSource == null) {
                throw new IllegalStateException("MessageSource is not set.");
            }
            return messageSource.getMessage(code);
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
                        .withZone(getTimeZone())
                        .print(dateTime);
            }
        }

        /**
         * @param dateTime to be formatted as UTC date/time
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
            return formatDateTime(dateTime, getTimeZone());
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
            }
            catch (ControllerReportSet.UserNotExistException exception) {
                AbstractNotification.logger.warn("User '{}' doesn't exist.", userId);
                return "<not-exist> (" + userId + ")";
            }
        }
    }
}
