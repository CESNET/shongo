package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.common.MessageSource;
import cz.cesnet.shongo.controller.settings.UserSettings;
import cz.cesnet.shongo.controller.settings.UserSettingsProvider;
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
     * Single default locale.
     */
    private static final List<Locale> DEFAULT_LOCALES = new LinkedList<Locale>(){{
        add(cz.cesnet.shongo.controller.api.UserSettings.LOCALE_ENGLISH);
    }};

    /**
     * @see UserSettingsProvider
     */
    private UserSettingsProvider userSettingsProvider;

    /**
     * List of {@link NotificationConfiguration}s for each recipient.
     */
    private Map<PersonInformation, List<NotificationConfiguration>> recipientConfigurations
            = new HashMap<PersonInformation, List<NotificationConfiguration>>();

    /**
     * {@link NotificationMessage} for each required {@link NotificationConfiguration}.
     */
    private Map<NotificationConfiguration, NotificationMessage> configurationMessage =
            new HashMap<NotificationConfiguration, NotificationMessage>();

    /**
     * Constructor.
     */
    public Notification()
    {
    }

    /**
     * Constructor.
     *
     * @param userSettingsProvider sets the {@link #userSettingsProvider}
     */
    public Notification(UserSettingsProvider userSettingsProvider)
    {
        this.userSettingsProvider = userSettingsProvider;
    }

    /**
     * @return collection of recipients who are configured for this {@link Notification}
     */
    public Collection<PersonInformation> getRecipients()
    {
        return recipientConfigurations.keySet();
    }

    /**
     * @return true whether {@link Notification} has configured at least one recipient,
     *         false otherwise
     */
    public boolean hasRecipients()
    {
        return recipientConfigurations.size() > 0;
    }

    /**
     * Remove all added recipients.
     */
    public void clearRecipients()
    {
        recipientConfigurations.clear();
    }

    /**
     * @param recipient     who should be notified by the {@link Notification}
     * @param administrator specifies whether {@code recipient} should be notified as administrator
     */
    public void addRecipient(PersonInformation recipient, boolean administrator)
    {
        Locale locale = null;
        DateTimeZone timeZone = null;
        if (recipient instanceof UserInformation && userSettingsProvider != null) {
            UserInformation userInformation = (UserInformation) recipient;
            UserSettings userSettings = userSettingsProvider.getUserSettings(userInformation.getUserId());
            if (userSettings != null) {
                locale = userSettings.getLocale();
                timeZone = userSettings.getTimeZone();
            }
        }
        if (timeZone == null) {
            timeZone = DateTimeZone.getDefault();
        }
        List<NotificationConfiguration> notificationConfigurations = new LinkedList<NotificationConfiguration>();
        if (locale == null) {
            for (Locale defaultLocale : getAvailableLocals()) {
                notificationConfigurations.add(new NotificationConfiguration(defaultLocale, timeZone, administrator));
            }
        }
        else {
            notificationConfigurations.add(new NotificationConfiguration(locale, timeZone, administrator));
        }
        recipientConfigurations.put(recipient, notificationConfigurations);
    }

    /**
     * @param recipients    who should be notified by the {@link Notification}
     * @param administrator specifies whether {@code recipients} should be notified as administrators
     */
    public void addRecipients(Collection<PersonInformation> recipients, boolean administrator)
    {
        for (PersonInformation recipient : recipients) {
            addRecipient(recipient, administrator);
        }
    }

    /**
     * @return list of available {@link Locale}s
     */
    protected List<Locale> getAvailableLocals()
    {
        return DEFAULT_LOCALES;
    }

    /**
     * @param recipient
     * @return {@link NotificationMessage} for given {@code recipient}
     */
    public NotificationMessage getRecipientMessage(PersonInformation recipient)
    {
        List<NotificationConfiguration> notificationConfigurations = recipientConfigurations.get(recipient);
        if (notificationConfigurations == null || notificationConfigurations.size() == 0) {
            throw new IllegalArgumentException("No configurations defined for recipient " + recipient + ".");
        }
        else if (notificationConfigurations.size() == 1) {
            // Single message
            return getRenderedMessage(notificationConfigurations.get(0));
        }
        else {
            // Multiple messages
            NotificationMessage notificationMessage = new NotificationMessage();
            for (NotificationConfiguration notificationConfiguration : notificationConfigurations) {
                NotificationMessage configurationMessage = getRenderedMessage(notificationConfiguration);
                notificationMessage.appendMessage(configurationMessage);
            }
            return notificationMessage;
        }
    }

    /**
     * Render or return already rendered {@link NotificationMessage} for given {@code configuration}.
     *
     * @param configuration to be rendered
     * @return rendered {@link NotificationMessage}
     */
    private NotificationMessage getRenderedMessage(NotificationConfiguration configuration)
    {
        NotificationMessage notificationMessage = configurationMessage.get(configuration);
        if (notificationMessage == null) {
            notificationMessage = renderMessage(configuration);
            configurationMessage.put(configuration, notificationMessage);
        }
        return notificationMessage;
    }

    /**
     * Render {@link NotificationMessage} from template with given {@code fileName}.
     *
     * @param renderContext
     * @param name
     * @param fileName
     * @return rendered {@link NotificationMessage}
     */
    protected NotificationMessage renderMessageTemplate(RenderContext renderContext, String name, String fileName)
    {
        Map<String, Object> templateParameters = new HashMap<String, Object>();
        templateParameters.put("context", renderContext);
        templateParameters.put("notification", this);
        for (Map.Entry<String, Object> parameter : renderContext.getParameters().entrySet()) {
            templateParameters.put(parameter.getKey(), parameter.getValue());
        }
        String content = renderTemplate(fileName, templateParameters);
        return new NotificationMessage(name, content);
    }

    /**
     * Render {@link NotificationMessage} for given {@code configuration}.
     *
     * @param configuration to be rendered
     * @return rendered {@link NotificationMessage}
     */
    protected abstract NotificationMessage renderMessage(NotificationConfiguration configuration);

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
     * Single instance of {@link Configuration}.
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
            templateConfiguration.setClassForTemplateLoading(Notification.class, "/");
        }
        return templateConfiguration;
    }

    /**
     * {@link Notification} context for rendering.
     */
    public static class RenderContext
    {
        /**
         * @see NotificationConfiguration
         */
        private NotificationConfiguration configuration;

        /**
         * Messages.
         */
        private MessageSource messageSource;

        /**
         * Parameters
         */
        private Map<String, Object> parameters = new HashMap<String, Object>();

        /**
         * Constructor.
         *
         * @param configuration sets the {@link #configuration}
         */
        public RenderContext(NotificationConfiguration configuration, String messageSourceFileName)
        {
            this.configuration = configuration;
            this.messageSource = new MessageSource(messageSourceFileName, configuration.getLocale());
        }

        /**
         * @return {@link #configuration#getTimeZone()}
         */
        public DateTimeZone getTimeZone()
        {
            return configuration.getTimeZone();
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
                return null;
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
                        .withZone(configuration.getTimeZone())
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
            return formatDateTime(dateTime, configuration.getTimeZone());
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
}
