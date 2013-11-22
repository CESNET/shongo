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
    public final static String IGNORE_DEFAULT_TIME_ZONE_ATTRIBUTE = "client-web.ignoreDefault.timeZone";

    /**
     * @see {@link UserSession#locale}
     */
    private Locale locale;

    /**
     * Specifies whether warning about default {@link #locale} should be displayed.
     */
    private boolean localeDefaultWarning;

    /**
     * @see {@link UserSession#timeZone}
     */
    private DateTimeZone timeZone;

    /**
     * Specifies whether warning about default {@link #timeZone} should be displayed.
     */
    private boolean timeZoneDefaultWarning;

    /**
     * Specifies whether current session is in {@link UserSettings#adminMode}.
     */
    private Boolean adminMode;

    /**
     * @see {@link #IGNORE_DEFAULT_TIME_ZONE_ATTRIBUTE}
     */
    private UserInterface userInterface;

    /**
     * Specifies whether {@link #userInterface} has been selected (and thus question should not be displayed).
     */
    private boolean userInterfaceSelected;

    /**
     * Constructor.
     *
     * @param userSettings to construct this {@link UserSettingsModel} from
     */
    public UserSettingsModel(UserSettings userSettings)
    {
        fromApi(userSettings);
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
     * @return {@link #timeZone}
     */
    public DateTimeZone getTimeZone()
    {
        return timeZone;
    }

    /**
     * @param timeZone sets the {@link #timeZone}
     */
    public void setTimeZone(DateTimeZone timeZone)
    {
        this.timeZone = timeZone;
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
     * @return {@link #adminMode}
     */
    public boolean isAdminMode()
    {
        return (adminMode != null ? adminMode : false);
    }

    /**
     * @return true whether {@link #adminMode} is available,
     *         false otherwise
     */
    public boolean isAdminModeAvailable()
    {
        return adminMode != null;
    }

    /**
     * @param adminMode sets the {@link #adminMode}
     */
    public void setAdminMode(Boolean adminMode)
    {
        this.adminMode = adminMode;
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
    }

    /**
     * @param userSettings to load this {@link UserSettingsModel} from
     */
    public void fromApi(UserSettings userSettings)
    {
        setLocale(userSettings.getLocale());
        setLocaleDefaultWarning(!userSettings.getAttributeBool(IGNORE_DEFAULT_LOCALE_ATTRIBUTE));
        setTimeZone(userSettings.getTimeZone());
        setTimeZoneDefaultWarning(!userSettings.getAttributeBool(IGNORE_DEFAULT_TIME_ZONE_ATTRIBUTE));
        setAdminMode(userSettings.getAdminMode());

        UserInterface userInterface = userSettings.getAttribute(USER_INTERFACE_ATTRIBUTE, UserInterface.class);
        if (userInterface != null) {
            setUserInterface(userInterface);
        }
        else {
            setUserInterface(DEFAULT_USER_INTERFACE);
        }
        setUserInterfaceSelected(userSettings.getAttributeBool(USER_INTERFACE_SELECTED_ATTRIBUTE));
    }

    /**
     * @return {@link UserSettings} from this {@link UserSettingsModel}
     */
    public UserSettings toApi()
    {
        UserSettings userSettings = new UserSettings();
        userSettings.setLocale(getLocale());
        userSettings.setTimeZone(getTimeZone());
        if (isAdminModeAvailable()) {
            userSettings.setAdminMode(isAdminMode());
        }
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
            userSettings.setAttribute(IGNORE_DEFAULT_TIME_ZONE_ATTRIBUTE, Boolean.TRUE.toString());
        }
        return userSettings;
    }

    @Override
    public String toContextString()
    {
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("Locale", locale);
        attributes.put("LocaleWarning", localeDefaultWarning);
        attributes.put("TimeZone", timeZone);
        attributes.put("TimeZoneWarning", timeZoneDefaultWarning);
        attributes.put("AdminMode", adminMode);
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
