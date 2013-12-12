package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.controller.api.UserSettings;
import org.joda.time.DateTimeZone;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Model for {@link UserSettings}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserSettingsModel implements ReportModel.ContextSerializable
{
    public final static UserInterface DEFAULT_USER_INTERFACE = UserInterface.BEGINNER;

    public final static String USER_INTERFACE_ATTRIBUTE = "client-web.userInterface";
    public final static String USER_INTERFACE_SELECTED_ATTRIBUTE = "client-web.userInterfaceSelected";
    public final static String IGNORE_DEFAULT_LOCALE_ATTRIBUTE = "client-web.ignoreDefault.locale";
    public final static String IGNORE_DEFAULT_HOME_TIME_ZONE_ATTRIBUTE = "client-web.ignoreDefault.homeTimeZone";

    /**
     * @see UserSettings#useWebService
     */
    private boolean useWebService;

    /**
     * @see {@link UserSession#locale}
     */
    private Locale locale;

    /**
     * Specifies whether warning about default {@link #locale} should be displayed.
     */
    private boolean localeDefaultWarning;

    /**
     * @see UserSettings#homeTimeZone
     */
    private DateTimeZone homeTimeZone;

    /**
     * @see UserSettings#currentTimeZone
     */
    private DateTimeZone currentTimeZone;

    /**
     * Specifies whether {@link #currentTimeZone} is not {@code null} and differs from {@link #homeTimeZone}.
     */
    private boolean currentTimeZoneEnabled;

    /**
     * Specifies whether warning about default time zone should be displayed.
     */
    private boolean timeZoneDefaultWarning;

    /**
     * @see UserSettings#administratorMode
     */
    private boolean administratorMode;

    /**
     * @see {@link #IGNORE_DEFAULT_HOME_TIME_ZONE_ATTRIBUTE}
     */
    private UserInterface userInterface;

    /**
     * Specifies whether {@link #userInterface} has been selected (and thus question should not be displayed).
     */
    private boolean userInterfaceSelected;

    /**
     * Constructor.
     *
     * @param userSettings to load from
     */
    public UserSettingsModel(UserSettings userSettings)
    {
        fromApi(userSettings);
    }

    /**
     * @return {@link #useWebService}
     */
    public boolean isUseWebService()
    {
        return useWebService;
    }

    /**
     * @param useWebService sets the {@link #useWebService}
     */
    public void setUseWebService(boolean useWebService)
    {
        this.useWebService = useWebService;
    }

    /**
     * @return {@link #locale}
     */
    public Locale getLocale()
    {
        return locale;
    }

    /**
     * @param locale sets the {@link #locale}
     */
    public void setLocale(Locale locale)
    {
        this.locale = locale;
    }

    /**
     * @return {@link #localeDefaultWarning}
     */
    public boolean isLocaleDefaultWarning()
    {
        return localeDefaultWarning;
    }

    /**
     * @param localeDefaultWarning sets the {@link #localeDefaultWarning}
     */
    public void setLocaleDefaultWarning(boolean localeDefaultWarning)
    {
        this.localeDefaultWarning = localeDefaultWarning;
    }

    /**
     * @return {@link #homeTimeZone}
     */
    public DateTimeZone getHomeTimeZone()
    {
        return homeTimeZone;
    }

    /**
     * @param homeTimeZone sets the {@link #homeTimeZone}
     */
    public void setHomeTimeZone(DateTimeZone homeTimeZone)
    {
        this.homeTimeZone = homeTimeZone;
    }

    /**
     * @return {@link #currentTimeZoneEnabled}
     */
    public boolean isCurrentTimeZoneEnabled()
    {
        return currentTimeZoneEnabled;
    }

    /**
     * @param currentTimeZoneEnabled sets the {@link #currentTimeZoneEnabled}
     */
    public void setCurrentTimeZoneEnabled(boolean currentTimeZoneEnabled)
    {
        this.currentTimeZoneEnabled = currentTimeZoneEnabled;
    }

    /**
     * @return {@link #currentTimeZone}
     */
    public DateTimeZone getCurrentTimeZone()
    {
        return currentTimeZone;
    }

    /**
     * @param currentTimeZone sets the {@link #currentTimeZone}
     */
    public void setCurrentTimeZone(DateTimeZone currentTimeZone)
    {
        this.currentTimeZone = currentTimeZone;
    }

    /**
     * @return {@link #timeZoneDefaultWarning}
     */
    public boolean isTimeZoneDefaultWarning()
    {
        return timeZoneDefaultWarning;
    }

    /**
     * @param timeZoneDefaultWarning sets the {@link #timeZoneDefaultWarning}
     */
    public void setTimeZoneDefaultWarning(boolean timeZoneDefaultWarning)
    {
        this.timeZoneDefaultWarning = timeZoneDefaultWarning;
    }

    /**
     * @return {@link #administratorMode}
     */
    public boolean isAdministratorMode()
    {
        return administratorMode;
    }

    /**
     * @param administratorMode sets the {@link #administratorMode}
     */
    public void setAdministratorMode(boolean administratorMode)
    {
        this.administratorMode = administratorMode;
    }

    /**
     * @return {@link #userInterface}
     */
    public UserInterface getUserInterface()
    {
        return userInterface;
    }

    /**
     * @param userInterface sets the {@link #userInterface}
     */
    public void setUserInterface(UserInterface userInterface)
    {
        this.userInterface = userInterface;
    }

    /**
     * @return {@link #userInterfaceSelected}
     */
    public boolean isUserInterfaceSelected()
    {
        return userInterfaceSelected;
    }

    /**
     * @param userInterfaceSelected sest the {@link #userInterfaceSelected}
     */
    public void setUserInterfaceSelected(boolean userInterfaceSelected)
    {
        this.userInterfaceSelected = userInterfaceSelected;
    }

    /**
     * @return true whether {@link #userInterface} is {@link UserInterface#ADVANCED},
     *         false otherwise
     */
    public boolean isAdvancedUserInterface()
    {
        return UserInterface.ADVANCED.equals(userInterface);
    }

    /**
     * @param advancedUserInterface sets the {@link #userInterface} to {@link UserInterface#ADVANCED}
     */
    public void setAdvancedUserInterface(boolean advancedUserInterface)
    {
        this.userInterface = advancedUserInterface ? UserInterface.ADVANCED : UserInterface.BEGINNER;
        this.userInterfaceSelected = true;
    }

    /**
     * @param userSettings to load this {@link UserSettingsModel} from
     */
    public void fromApi(UserSettings userSettings)
    {
        this.useWebService = userSettings.isUseWebService();
        this.locale = userSettings.getLocale();
        this.localeDefaultWarning = !userSettings.getAttributeBool(IGNORE_DEFAULT_LOCALE_ATTRIBUTE);
        this.homeTimeZone = userSettings.getHomeTimeZone();
        this.timeZoneDefaultWarning = !userSettings.getAttributeBool(IGNORE_DEFAULT_HOME_TIME_ZONE_ATTRIBUTE);
        this.currentTimeZone = userSettings.getCurrentTimeZone();
        this.currentTimeZoneEnabled = (this.currentTimeZone != null && !this.currentTimeZone.equals(this.homeTimeZone));
        this.administratorMode = userSettings.getAdministratorMode();

        UserInterface userInterface = userSettings.getAttribute(USER_INTERFACE_ATTRIBUTE, UserInterface.class);
        if (userInterface != null) {
            this.userInterface = userInterface;
        }
        else {
            this.userInterface = DEFAULT_USER_INTERFACE;
        }
        this.userInterfaceSelected = userSettings.getAttributeBool(USER_INTERFACE_SELECTED_ATTRIBUTE);
    }

    /**
     * @return {@link UserSettings} from this {@link UserSettingsModel}
     */
    public UserSettings toApi()
    {

        UserSettings userSettings = new UserSettings();
        userSettings.setUseWebService(useWebService);
        if (!useWebService) {
            userSettings.setLocale(locale);
        }
        userSettings.setHomeTimeZone(homeTimeZone);
        if (currentTimeZoneEnabled) {
            userSettings.setCurrentTimeZone(currentTimeZone);
        }
        userSettings.setAdministratorMode(administratorMode);
        UserInterface userInterface = getUserInterface();
        if (userInterface != null) {
            userSettings.setAttribute(USER_INTERFACE_ATTRIBUTE, userInterface.toString());
        }
        if (userInterfaceSelected) {
            userSettings.setAttribute(USER_INTERFACE_SELECTED_ATTRIBUTE, Boolean.TRUE.toString());
        }
        if (!isLocaleDefaultWarning()) {
            userSettings.setAttribute(IGNORE_DEFAULT_LOCALE_ATTRIBUTE, Boolean.TRUE.toString());
        }
        if (!isTimeZoneDefaultWarning()) {
            userSettings.setAttribute(IGNORE_DEFAULT_HOME_TIME_ZONE_ATTRIBUTE, Boolean.TRUE.toString());
        }
        return userSettings;
    }

    @Override
    public String toContextString()
    {
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("UseWebService", useWebService);
        attributes.put("Locale", locale);
        attributes.put("LocaleWarning", localeDefaultWarning);
        attributes.put("HomeTimeZone", homeTimeZone);
        attributes.put("HomeTimeZoneWarning", timeZoneDefaultWarning);
        attributes.put("CurrentTimeZone", currentTimeZone);
        attributes.put("AdminMode", administratorMode);
        attributes.put("UserInterface", userInterface);
        return ReportModel.formatAttributes(attributes);
    }

    /**
     * Type of user interface.
     */
    public static enum UserInterface
    {
        /**
         * UI for beginners.
         */
        BEGINNER,

        /**
         * UI for advanced users.
         */
        ADVANCED
    }
}
