package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.api.AbstractPerson;
import cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.util.DateTimeFormatter;
import cz.cesnet.shongo.util.MessageSource;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.PeriodFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.io.StringWriter;
import java.util.*;

/**
 * Represents a notification for a list of recipients.
 * <p/>
 * The {@link #renderMessage} abstract method must be implemented to render {@link NotificationMessage}
 * for each recipient. You can use {@link #renderTemplateMessage} method for the rendering based on templates.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractNotification
{
    protected static Logger logger = LoggerFactory.getLogger(AbstractNotification.class);

    /**
     * List of recipients who should be notified about this {@link AbstractNotification}.
     */
    private final Set<PersonInformation> recipients = new LinkedHashSet<PersonInformation>();

    /**
     * List of reply-to who should be contacted when replying to notification about this {@link AbstractNotification}.
     */
    private final Set<PersonInformation> replyTo = new HashSet<PersonInformation>();

    /**
     * Constructor.
     */
    protected AbstractNotification()
    {
    }

    /**
     * @param recipient who should be notified by the {@link AbstractNotification}
     * @return true whether given {@code recipient} has been added,
     * false whether given {@code recipient} already exists
     */
    protected boolean addRecipient(PersonInformation recipient)
    {
        return recipients.add(recipient);
    }

    /**
     * @param recipients to be added by {@link #addRecipient}
     */
    protected final void addRecipients(Collection<PersonInformation> recipients)
    {
        for (PersonInformation recipient : recipients) {
            addRecipient(recipient);
        }
    }

    /**
     * @param recipient who should not be notified by the {@link AbstractNotification}
     * @return true whether given {@code recipient} has been removed,
     * false whether given {@code recipient} didn't exist
     */
    public boolean removeRecipient(PersonInformation recipient)
    {
        return recipients.remove(recipient);
    }

    /**
     * @param recipients to be removed by the {@link #removeRecipient}
     */
    public final void removeRecipients(Set<PersonInformation> recipients)
    {
        for (PersonInformation recipient : recipients) {
            removeRecipient(recipient);
        }
    }

    /**
     * Remove all recipients from the {@link #recipients}.
     */
    public void clearRecipients()
    {
        recipients.clear();
    }

    /**
     * @return {@link #recipients}
     */
    public Set<PersonInformation> getRecipients()
    {
        return Collections.unmodifiableSet(recipients);
    }

    /**
     * @return true whether {@link #recipients} is not empty, false otherwise
     */
    public final boolean hasRecipients()
    {
        return !recipients.isEmpty();
    }

    /**
     * @param replyTo to be added to the {@link #replyTo}
     */
    protected boolean addReplyTo(PersonInformation replyTo)
    {
        return this.replyTo.add(replyTo);
    }

    /**
     * @return {@link #replyTo}
     */
    public Set<PersonInformation> getReplyTo()
    {
        return Collections.unmodifiableSet(replyTo);
    }

    /**
     * @param recipient     for who the message should be retrieved
     * @param manager       to be used
     * @param entityManager
     * @return {@link NotificationMessage} for given {@code recipient}
     */
    public final NotificationMessage getMessage(PersonInformation recipient, NotificationManager manager,
            EntityManager entityManager)
    {
        return renderMessage(recipient, manager, entityManager);
    }

    /**
     * Render {@link NotificationMessage} for given {@code recipient}.
     *
     * @param recipient     for who the message should be rendered
     * @param manager
     * @param entityManager
     * @return rendered {@link NotificationMessage}
     */
    protected abstract NotificationMessage renderMessage(PersonInformation recipient, NotificationManager manager,
            EntityManager entityManager);

    /**
     * Render {@link NotificationMessage} from template with given {@code fileName}.
     *
     * @param context  to be used for rendering
     * @param title    message title
     * @param fileName message template filename
     * @return rendered {@link NotificationMessage}
     */
    protected final NotificationMessage renderTemplateMessage(RenderContext context, String title, String fileName)
    {
        String content = renderTemplate(context, fileName);
        return new NotificationMessage(context.getLanguage(), title, content);
    }

    /**
     * Render template with given {@code fileName}.
     *
     * @param context  to be used for rendering
     * @param fileName message template filename
     * @return rendered {@link NotificationMessage}
     */
    protected final String renderTemplate(RenderContext context, String fileName)
    {
        Map<String, Object> templateParameters = new HashMap<String, Object>();
        templateParameters.put("context", context);
        templateParameters.put("notification", this);
        for (Map.Entry<String, Object> parameter : context.getParameters().entrySet()) {
            templateParameters.put(parameter.getKey(), parameter.getValue());
        }
        return renderTemplate(fileName, templateParameters);
    }

    /**
     * Event called before this {@link AbstractNotification} is added to the {@link NotificationManager}.
     *
     * @param notificationManager
     * @param entityManager
     * @return true whether this {@link AbstractNotification} should be added to the {@code notificationManager},
     * false otherwise
     */
    protected boolean onBeforeAdded(NotificationManager notificationManager, EntityManager entityManager)
    {
        return true;
    }

    /**
     * Event called after this {@link AbstractNotification} has been added to the {@link NotificationManager}.
     *
     * @param notificationManager
     * @param entityManager
     */
    protected void onAfterAdded(NotificationManager notificationManager, EntityManager entityManager)
    {
    }

    /**
     * Event called right after this {@link AbstractNotification} has been removed from the {@link NotificationManager}.
     *
     * @param notificationManager
     */
    protected void onAfterRemoved(NotificationManager notificationManager)
    {
    }

    /**
     * Preprocess this {@link AbstractNotification}.
     *
     * @param notificationManager to be used
     * @return true when this {@link AbstractNotification} should be deleted and not executed,
     * false otherwise
     */
    public boolean preprocess(NotificationManager notificationManager)
    {
        return false;
    }

    @Override
    public String toString()
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
         * @return {@link Locale}
         */
        public Locale getLocale()
        {
            return Locale.getDefault();
        }

        /**
         * @return {@link #getLocale()#getLanguage()}
         */
        public final String getLanguage()
        {
            return getLocale().getLanguage();
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
         * @param width
         * @param value
         * @return value with given {@code width} (aligned right)
         */
        public String width(int width, String value)
        {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(value);
            for (int length = stringBuilder.length(); length < width; length++) {
                stringBuilder.insert(0, " ");
            }
            return stringBuilder.toString();
        }

        /**
         * @param width
         * @return space with given {@code width}
         */
        public String width(int width)
        {
            return width(width, "");
        }

        /**
         * @param size
         * @return indent given {@code text} by given {@code size}
         */
        public String indent(int size, String text)
        {
            String indent = width(size);
            return indent + text.replaceAll("\n", "\n" + indent);
        }

        /**
         * @param size
         * @return indent given {@code text} by given {@code size}
         */
        public String indentNextLines(int size, String text)
        {
            String indent = width(size);
            return text.replaceAll("\n", "\n" + indent);
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
         * @param code
         * @param arguments
         * @return message for given {@code code}
         */
        public String message(String code, Object... arguments)
        {
            if (messageSource == null) {
                throw new IllegalStateException("MessageSource is not set.");
            }
            return messageSource.getMessage(code, arguments);
        }

        /**
         * @param width
         * @param code
         * @return message for given {@code code}
         */
        public String message(int width, String code)
        {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(message(code));
            for (int length = stringBuilder.length(); length < width; length++) {
                stringBuilder.insert(0, " ");
            }
            return stringBuilder.toString();
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
                        .withZone(timeZone)
                        .print(dateTime);
            }
        }

        /**
         * @param dateTime to be formatted as time
         * @param timeZone to be used
         * @return {@code dateTime} formatted to string
         */
        public String formatTime(DateTime dateTime, DateTimeZone timeZone)
        {
            return DateTimeFormat.forPattern("HH:mm").withZone(timeZone).print(dateTime);
        }

        /**
         * @param dateTime to be formatted as date/time
         * @param timeZone to be used
         * @return {@code dateTime} formatted to string
         */
        public String formatDateTimeWithoutZone(DateTime dateTime, DateTimeZone timeZone)
        {
            if (dateTime.equals(Temporal.DATETIME_INFINITY_START) || dateTime.equals(Temporal.DATETIME_INFINITY_END)) {
                return "(infinity)";
            }
            else {
                return DateTimeFormat.forPattern("d.M.yyyy HH:mm").withZone(timeZone).print(dateTime);
            }
        }

        /**
         * @param dateTime to be formatted as date/time
         * @param timeZone to be used
         * @return {@code dateTime} formatted to string
         */
        public String formatDateTime(DateTime dateTime, DateTimeZone timeZone)
        {
            return String.format("%s (%s)",
                    formatDateTimeWithoutZone(dateTime, timeZone),
                    formatTimeZone(timeZone, dateTime));
        }

        /**
         * @param dateTime   to be formatted as date/time
         * @param timeZoneId to be used
         * @return {@code dateTime} formatted to string
         */
        public String formatDateTime(DateTime dateTime, String timeZoneId)
        {
            DateTimeZone dateTimeZone = DateTimeZone.forID(timeZoneId);
            return formatDateTime(dateTime, dateTimeZone);
        }

        /**
         * @param interval   whose start to be formatted as date/time
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
         * @param interval to be formatted
         * @param timeZone to be used
         * @return {@code interval} formatted to string
         */
        public String formatInterval(Interval interval, DateTimeZone timeZone)
        {
            DateTime start = interval.getStart();
            DateTime end = interval.getEnd();
            if (start.withTimeAtStartOfDay().equals(end.withTimeAtStartOfDay())) {
                return String.format("%s - %s (%s)",
                        formatDateTimeWithoutZone(start, timeZone),
                        formatTime(end, timeZone),
                        formatTimeZone(timeZone, start));
            }
            else {
                return String.format("%s - %s (%s)",
                        formatDateTimeWithoutZone(start, timeZone),
                        formatDateTimeWithoutZone(end, timeZone),
                        formatTimeZone(timeZone, start));
            }
        }

        public String formatDate(ReadablePartial date)
        {
            return DateTimeFormat.mediumDate().withLocale(getLocale()).print(date);
        }

        public String formatPeriod(Period period)
        {
            DateTimeFormatter formatter = DateTimeFormatter.getInstance(DateTimeFormatter.Type.LONG, getLocale(), null);
            return formatter.formatDuration(period);
        }

        /**
         * @param interval   to be formatted
         * @param timeZoneId to be used
         * @return {@code interval} formatted to string
         */
        public String formatInterval(Interval interval, String timeZoneId)
        {
            DateTimeZone timeZone = DateTimeZone.forID(timeZoneId);
            return formatInterval(interval, timeZone);
        }

        /**
         * @param interval to be formatted
         * @return {@code interval} formatted to string
         */
        public String formatInterval(Interval interval)
        {
            return formatInterval(interval, getTimeZone());
        }

        /**
         * @param slot to be formatted
         * @return {@code interval} formatted to string
         */
        public String formatPeriodicSlot(PeriodicDateTimeSlot slot)
        {
            DateTime start = slot.getStart();
            DateTime end = start.plus(slot.getDuration());
            Interval interval = new Interval(start,end);
            return formatInterval(interval, getTimeZone());
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
            return DateTimeFormatter.getInstance(DateTimeFormatter.Type.SHORT).with(getLocale()).formatDuration(
                    DateTimeFormatter.roundDuration(duration));
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
         * @return {@link cz.cesnet.shongo.controller.booking.person.AbstractPerson} for given {@code userId}
         */
        public AbstractPerson getUserPerson(String userId)
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
            catch (ControllerReportSet.UserNotExistsException exception) {
                AbstractNotification.logger.warn("User '{}' doesn't exist.", userId);
                return "<not-exist> (" + userId + ")";
            }
        }

        public boolean isPeriodic(Period period)
        {
            if (period == null || Period.ZERO.equals(period)) {
                return false;
            }
            return true;
        }
    }
}
