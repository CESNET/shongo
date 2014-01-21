package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.api.UserSettings;
import cz.cesnet.shongo.controller.settings.UserSettingsManager;
import cz.cesnet.shongo.util.MessageSource;
import org.joda.time.DateTimeZone;

import java.util.*;

/**
 * {@link AbstractNotification} which can render {@link NotificationMessage} in multiple {@link Configuration}s
 * (e.g., in all available languages for users who doesn't prefer any language or
 * with/without administrator information).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ConfigurableNotification extends AbstractNotification
{
    /**
     * @see cz.cesnet.shongo.controller.ControllerConfiguration
     */
    protected ControllerConfiguration configuration;

    /**
     * @see cz.cesnet.shongo.controller.settings.UserSettingsManager
     */
    private UserSettingsManager userSettingsManager;

    /**
     * List of {@link ConfigurableNotification.Configuration}s for each recipient.
     */
    private Map<PersonInformation, List<Configuration>> recipientConfigurations =
            new HashMap<PersonInformation, List<Configuration>>();

    /**
     * {@link NotificationMessage} for each required {@link Configuration}.
     */
    private Map<Configuration, NotificationMessage> configurationMessage =
            new HashMap<Configuration, NotificationMessage>();

    /**
     * Constructor.
     *
     * @param userSettingsManager sets the {@link #userSettingsManager}
     * @param configuration       sets the {@link #configuration}
     */
    public ConfigurableNotification(UserSettingsManager userSettingsManager, ControllerConfiguration configuration)
    {
        this.userSettingsManager = userSettingsManager;
        this.configuration = configuration;
    }

    /**
     * Constructor.
     *
     * @param recipients          sets the {@link #recipients}
     * @param userSettingsManager sets the {@link #userSettingsManager}
     * @param configuration       sets the {@link #configuration}
     */
    public ConfigurableNotification(Collection<PersonInformation> recipients, UserSettingsManager userSettingsManager,
            ControllerConfiguration configuration)
    {
        this.userSettingsManager = userSettingsManager;
        this.configuration = configuration;
        addRecipients(recipients);
    }

    /**
     * @return list of available {@link Locale}s for this {@link ConfigurableNotification}
     */
    protected abstract Collection<Locale> getAvailableLocals();

    /**
     * @param locale
     * @param timeZone
     * @param administrator
     * @return new instance of {@link Configuration}
     */
    protected Configuration createConfiguration(Locale locale, DateTimeZone timeZone, boolean administrator)
    {
        return new Configuration(locale, timeZone, administrator);
    }

    /**
     * @param recipient     who should be notified by the {@link ConfigurableNotification}
     * @param administrator specifies whether {@code recipient} should be notified as administrator
     * @return true whether given {@code recipient} has been added,
     *         false whether given {@code recipient} already exists
     */
    protected boolean addRecipient(PersonInformation recipient, boolean administrator)
    {
        if (!super.addRecipient(recipient)) {
            return false;
        }

        // Determine recipient locale and timezone
        Locale locale = null;
        DateTimeZone timeZone = null;
        if (recipient instanceof UserInformation && userSettingsManager != null) {
            UserInformation userInformation = (UserInformation) recipient;
            UserSettings userSettings = userSettingsManager.getUserSettings(userInformation.getUserId());
            if (userSettings != null) {
                locale = userSettings.getLocale();
                timeZone = userSettings.getCurrentTimeZone();
                if (timeZone == null) {
                    timeZone = userSettings.getHomeTimeZone();
                }
            }
        }

        // Get default timezone
        if (timeZone == null) {
            timeZone = DateTimeZone.getDefault();
        }

        // Get single or multiple configurations
        List<Configuration> configurations = new LinkedList<Configuration>();
        if (locale == null) {
            for (Locale defaultLocale : getAvailableLocals()) {
                configurations.add(createConfiguration(defaultLocale, timeZone, administrator));
            }
        }
        else {
            configurations.add(createConfiguration(locale, timeZone, administrator));
        }

        // Add configurations for the recipient
        recipientConfigurations.put(recipient, configurations);
        return true;
    }

    /**
     * @param recipients    who should be notified by the {@link ConfigurableNotification}
     * @param administrator specifies whether {@code recipients} should be notified as administrators
     */
    public final void addRecipients(Collection<PersonInformation> recipients, boolean administrator)
    {
        for (PersonInformation recipient : recipients) {
            addRecipient(recipient, administrator);
        }
    }

    @Override
    public boolean addRecipient(PersonInformation recipient)
    {
        // Add the recipient as not-administrator
        return addRecipient(recipient, false);
    }

    @Override
    protected NotificationMessage renderMessageForRecipient(PersonInformation recipient)
    {
        List<Configuration> configurations = recipientConfigurations.get(recipient);
        if (configurations == null || configurations.size() == 0) {
            throw new IllegalArgumentException("No configurations defined for recipient " + recipient + ".");
        }
        else if (configurations.size() == 1) {
            // Single message
            return getRenderedMessageForConfiguration(configurations.get(0)).clone();
        }
        else {
            // Multiple messages
            NotificationMessage notificationMessage = new NotificationMessage(recipient, configuration);
            for (Configuration configuration : configurations) {
                NotificationMessage configurationMessage = getRenderedMessageForConfiguration(configuration);
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
    private NotificationMessage getRenderedMessageForConfiguration(Configuration configuration)
    {
        NotificationMessage notificationMessage = configurationMessage.get(configuration);
        if (notificationMessage == null) {
            notificationMessage = renderMessageForConfiguration(configuration);
            configurationMessage.put(configuration, notificationMessage);
        }
        return notificationMessage;
    }

    /**
     * Render {@link NotificationMessage} for given {@code configuration}.
     *
     * @param configuration to be rendered
     * @return rendered {@link NotificationMessage}
     */
    protected abstract NotificationMessage renderMessageForConfiguration(Configuration configuration);

    /**
     * {@link RenderContext} for rendering of {@link ConfigurableNotification}.
     */
    public static class ConfiguredRenderContext extends RenderContext
    {
        /**
         * @see cz.cesnet.shongo.controller.ControllerConfiguration#NOTIFICATION_USER_SETTINGS_URL
         */
        private String userSettingsUrl;

        /**
         * @see ConfigurableNotification.Configuration
         */
        private Configuration configuration;

        /**
         * Constructor.
         *
         * @param configuration         sets the {@link #configuration}
         * @param messageSourceFileName
         * @param userSettingsUrl
         */
        public ConfiguredRenderContext(Configuration configuration, String messageSourceFileName,
                String userSettingsUrl)
        {
            super(new MessageSource("notification/" + messageSourceFileName, configuration.getLocale()));

            this.configuration = configuration;
            this.userSettingsUrl = userSettingsUrl;
        }

        /**
         * @return {@link #configuration}
         */
        public Configuration getConfiguration()
        {
            return configuration;
        }

        /**
         * @return {@link #configuration#isAdministator()}
         */
        public boolean isAdministrator()
        {
            return configuration.isAdministrator();
        }

        @Override
        public Locale getLocale()
        {
            return configuration.getLocale();
        }

        @Override
        public DateTimeZone getTimeZone()
        {
            return configuration.getTimeZone();
        }

        /**
         * @return true whether {@link #configuration#getTimeZone()} is default {@link DateTimeZone},
         *         false otherwise
         */
        public boolean isTimeZoneDefault()
        {
            return configuration.getTimeZone().equals(DateTimeZone.getDefault());
        }

        /**
         * @return {@link #userSettingsUrl}
         */
        public String getUserSettingsUrl()
        {
            return userSettingsUrl;
        }
    }

    /**
     * Represents a single configuration for rendering of a {@link ConfigurableNotification}.
     * {@link ConfigurableNotification} can be rendered for one or more {@link Configuration}s for each recipient.
     */
    public static class Configuration
    {
        /**
         * Specifies locale (e.g., for language).
         */
        private final Locale locale;

        /**
         * Specifies timezone for date/times.
         */
        private final DateTimeZone timeZone;

        /**
         * Specifies whether {@link ConfigurableNotification} should render administrator information.
         */
        private final boolean administrator;

        /**
         * Constructor.
         *
         * @param locale        sets the {@link #locale}
         * @param timeZone      sets the {@link #timeZone}
         * @param administrator sets the {@link #administrator}
         */
        public Configuration(Locale locale, DateTimeZone timeZone, boolean administrator)
        {
            this.locale = locale;
            this.timeZone = timeZone;
            this.administrator = administrator;
        }

        /**
         * @return {@link #locale}
         */
        public Locale getLocale()
        {
            return locale;
        }

        /**
         * @return {@link #timeZone}
         */
        public DateTimeZone getTimeZone()
        {
            return timeZone;
        }

        /**
         * @return {@link #administrator}
         */
        public boolean isAdministrator()
        {
            return administrator;
        }

        @Override
        public boolean equals(Object object)
        {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            Configuration configuration = (Configuration) object;
            if (administrator != configuration.administrator) {
                return false;
            }
            if (!locale.equals(configuration.locale)) {
                return false;
            }
            if (!timeZone.equals(configuration.timeZone)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode()
        {
            int result = locale.hashCode();
            result = 31 * result + timeZone.hashCode();
            result = 31 * result + (administrator ? 1 : 0);
            return result;
        }
    }

    /**
     * {@link Configuration} for parent {@link ConfigurableNotification}s which have child {@link ConfigurableNotification}s
     * which should be able to detect whether they are rendering as standalone or inside parent event.
     * <p/>
     * We need a new class of {@link ConfigurableNotification.Configuration} because we want
     * the child {@link ConfigurableNotification}s to render in a different way when they are rendered from parent event class
     * (rendered content is cached by equal {@link ConfigurableNotification.Configuration}s).
     */
    public static class ParentConfiguration extends Configuration
    {
        /**
         * Constructor.
         *
         * @param locale        sets the {@link #locale}
         * @param timeZone      sets the {@link #timeZone}
         * @param administrator sets the {@link #administrator}
         */
        public ParentConfiguration(Locale locale, DateTimeZone timeZone, boolean administrator)
        {
            super(locale, timeZone, administrator);
        }
    }
}
