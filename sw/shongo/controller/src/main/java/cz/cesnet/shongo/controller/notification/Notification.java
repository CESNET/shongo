package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.settings.UserSettings;
import cz.cesnet.shongo.controller.settings.UserSettingsProvider;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import org.joda.time.DateTimeZone;
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
     * @param configuration to be used in the {@link NotificationRenderContext}
     * @return {@link NotificationRenderContext}
     */
    protected NotificationRenderContext createRenderContext(NotificationConfiguration configuration)
    {
        return new NotificationRenderContext(configuration);
    }

    /**
     * Render {@link NotificationMessage} from given {@code messageTemplateFileName}.
     *
     * @param configuration
     * @param name
     * @param templateFileName
     * @param templateParameters
     * @return rendered {@link NotificationMessage}
     */
    protected NotificationMessage renderMessageTemplate(NotificationConfiguration configuration,
            String name, String templateFileName, Map<String, Object> templateParameters, String messagesFileName)
    {
        if (templateParameters == null) {
            templateParameters = new HashMap<String, Object>();
        }
        NotificationRenderContext renderContext = createRenderContext(configuration);
        renderContext.setMessages(messagesFileName);
        templateParameters.put("context", renderContext);
        templateParameters.put("notification", this);
        String content = renderTemplate(templateFileName, templateParameters);
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
}
