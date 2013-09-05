package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.common.MessageSource;
import cz.cesnet.shongo.controller.notification.manager.NotificationManager;
import cz.cesnet.shongo.controller.settings.UserSettings;
import cz.cesnet.shongo.controller.settings.UserSettingsProvider;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Represents an abstract {@link AbstractNotification} which can render {@link NotificationMessage}
 * in multiple {@link Configuration}s (e.g., in all available languages for users which doesn't prefer any language
 * or with/without administrator information).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ConfigurableNotification extends AbstractNotification
{
    protected static Logger logger = LoggerFactory.getLogger(NotificationManager.class);

    /**
     * @see cz.cesnet.shongo.controller.settings.UserSettingsProvider
     */
    private UserSettingsProvider userSettingsProvider;

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
     * @param userSettingsProvider sets the {@link #userSettingsProvider}
     */
    public ConfigurableNotification(UserSettingsProvider userSettingsProvider)
    {
        this.userSettingsProvider = userSettingsProvider;
    }

    /**
     * @return list of available {@link Locale}s for this {@link ConfigurableNotification}
     */
    protected abstract List<Locale> getAvailableLocals();

    /**
     * @param recipient     who should be notified by the {@link ConfigurableNotification}
     * @param administrator specifies whether {@code recipient} should be notified as administrator
     * @return true whether given {@code recipient} has been added,
     *         false whether given {@code recipient} already exists
     */
    public boolean addRecipient(PersonInformation recipient, boolean administrator)
    {
        if (!super.addRecipient(recipient)) {
            return false;
        }

        // Determine recipient locale and timezone
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

        // Get default timezone
        if (timeZone == null) {
            timeZone = DateTimeZone.getDefault();
        }

        // Get single or multiple configurations
        List<Configuration> configurations = new LinkedList<Configuration>();
        if (locale == null) {
            for (Locale defaultLocale : getAvailableLocals()) {
                configurations.add(new Configuration(defaultLocale, timeZone, administrator));
            }
        }
        else {
            configurations.add(new Configuration(locale, timeZone, administrator));
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
    public void clearRecipients()
    {
        super.clearRecipients();

        recipientConfigurations.clear();
    }

    @Override
    protected final NotificationMessage renderMessageForRecipient(PersonInformation recipient)
    {
        List<Configuration> configurations = recipientConfigurations.get(recipient);
        if (configurations == null || configurations.size() == 0) {
            throw new IllegalArgumentException("No configurations defined for recipient " + recipient + ".");
        }
        else if (configurations.size() == 1) {
            // Single message
            return getRenderedMessageForConfiguration(configurations.get(0));
        }
        else {
            // Multiple messages
            NotificationMessage notificationMessage = new NotificationMessage();
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
         * @see ConfigurableNotification.Configuration
         */
        private Configuration configuration;

        /**
         * Constructor.
         *
         * @param configuration sets the {@link #configuration}
         */
        public ConfiguredRenderContext(Configuration configuration, String messageSourceFileName)
        {
            super(new MessageSource(messageSourceFileName, configuration.getLocale()));

            this.configuration = configuration;
        }

        @Override
        public DateTimeZone getTimeZone()
        {
            return configuration.getTimeZone();
        }
    }

    /**
     * Represents a single configuration for rendering of a {@link ConfigurableNotification}.
     * {@link ConfigurableNotification} can be rendered for one or more {@link Configuration}s for each recipient.
     */
    public class Configuration
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
}
