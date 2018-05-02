package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.api.Converter;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.api.UserSettings;
import cz.cesnet.shongo.controller.settings.UserSettingsManager;
import cz.cesnet.shongo.util.MessageSource;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.ReadablePartial;

import javax.persistence.EntityManager;
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
     * Administrator recipients.
     */
    private Set<PersonInformation> administratorRecipients = new HashSet<PersonInformation>();

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
     */
    public ConfigurableNotification()
    {
    }

    /**
     * Constructor.
     *
     * @param recipients sets the {@link #recipients}
     */
    public ConfigurableNotification(Collection<PersonInformation> recipients)
    {
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

        if (administrator) {
            administratorRecipients.add(recipient);
        }
        else {
            administratorRecipients.remove(recipient);
        }

        return true;
    }

    /**
     * @param recipient to be set as administrator
     */
    public void setRecipientAsAdministrator(PersonInformation recipient)
    {
        administratorRecipients.add(recipient);
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
    public boolean removeRecipient(PersonInformation recipient)
    {
        administratorRecipients.remove(recipient);
        return super.removeRecipient(recipient);
    }

    @Override
    protected NotificationMessage renderMessage(PersonInformation recipient, NotificationManager notificationManager,
            EntityManager entityManager)
    {
        // Get configurations
        List<Configuration> configurations = recipientConfigurations.get(recipient);
        if (configurations == null) {
            // Determine whether recipient is administrator
            boolean administrator = administratorRecipients.contains(recipient);

            // Determine recipient locale and timezone
            Locale locale = null;
            DateTimeZone timeZone = null;
            if (recipient instanceof UserInformation) {
                UserInformation userInformation = (UserInformation) recipient;
                UserSettingsManager userSettingsManager = new UserSettingsManager(
                        entityManager, notificationManager.getAuthorization());
                UserSettings userSettings = userSettingsManager.getUserSettings(userInformation.getUserId(), null);
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
            configurations = new LinkedList<Configuration>();
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
        }

        // Render message for each configuration
        if (configurations.size() == 1) {
            // Single message
            try {
                return getRenderedMessage(recipient, configurations.get(0), notificationManager).clone();
            }
            catch (CloneNotSupportedException exception) {
                exception.printStackTrace();
                throw new RuntimeException(exception);
            }
        }
        else {
            // Multiple messages
            NotificationMessage notificationMessage = new NotificationMessage(recipient, notificationManager);
            for (Configuration configuration : configurations) {
                NotificationMessage configurationMessage =
                        getRenderedMessage(recipient, configuration, notificationManager);
                notificationMessage.appendMessage(configurationMessage);
            }
            return notificationMessage;
        }
    }

    /**
     * Render or return already rendered {@link NotificationMessage} for given {@code configuration}.
     *
     *
     * @param recipient
     * @param configuration to be rendered
     * @param manager
     * @return rendered {@link NotificationMessage}
     */
    protected NotificationMessage getRenderedMessage(PersonInformation recipient, Configuration configuration,
            NotificationManager manager)
    {
        NotificationMessage notificationMessage = configurationMessage.get(configuration);
        if (notificationMessage == null) {
            notificationMessage = renderMessage(configuration, manager);
            configurationMessage.put(configuration, notificationMessage);
        }
        return notificationMessage;
    }

    /**
     * Render {@link NotificationMessage} for given {@code configuration}.
     *
     * @param configuration to be rendered
     * @param manager
     * @return rendered {@link NotificationMessage}
     */
    protected abstract NotificationMessage renderMessage(Configuration configuration, NotificationManager manager);

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
         * @see ConfigurableNotification.Configuration
         */
        private ControllerConfiguration controllerConfiguration;

        /**
         * Constructor.
         *
         * @param configuration           sets the {@link #configuration}
         * @param messageSourceFileName
         * @param notificationManager
         */
        public ConfiguredRenderContext(Configuration configuration, String messageSourceFileName,
                NotificationManager notificationManager)
        {
            super(new MessageSource("notification/" + messageSourceFileName, configuration.getLocale()));

            this.configuration = configuration;
            this.controllerConfiguration = notificationManager.getConfiguration();
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
         * @return {@link ControllerConfiguration#NOTIFICATION_USER_SETTINGS_URL}
         */
        public String getUserSettingsUrl()
        {
            return controllerConfiguration.getNotificationUserSettingsUrl();
        }

        /**
         * @return {@link ControllerConfiguration#NOTIFICATION_RESERVATION_REQUEST_URL}
         */
        public String getReservationRequestUrl()
        {
            return controllerConfiguration.getNotificationReservationRequestUrl();
        }

        /**
         * @return {@link ControllerConfiguration#NOTIFICATION_RESERVATION_REQUEST_URL}
         */
        public String getReservationRequestConfirmationUrl()
        {
            return controllerConfiguration.getNotificationReservationRequestConfirmationUrl();
        }

        /**
         * @return {@link ControllerConfiguration#FREEPBX_PDF_GUIDE_FILEPATH}
         */
        public String getFreePBXPDFGuidePath()
        {
            return controllerConfiguration.getFreePBXPDFGuidePath();
        }

        /**
         * @param reservationRequestId
         * @return {@link ControllerConfiguration#NOTIFICATION_RESERVATION_REQUEST_URL} with given {@code reservationRequestId}
         */
        public String formatReservationRequestUrl(String reservationRequestId)
        {
            String reservationRequestUrl = getReservationRequestUrl();
            return reservationRequestUrl.replace("${reservationRequestId}", reservationRequestId);
        }


        /**
         * @param resourceId
         * @return {@link ControllerConfiguration#NOTIFICATION_RESERVATION_REQUEST_URL} with given {@code reservationRequestId}
         */
        public String formatReservationRequestConfirmationUrl(String resourceId, Interval interval)
        {
            String reservationRequestConfirmationUrl = getReservationRequestConfirmationUrl();
            String date = Converter.convertLocalDateToString(interval.getStart().toLocalDate());
            return reservationRequestConfirmationUrl.replace("${resourceId}", resourceId).replace("${date}", date);
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
