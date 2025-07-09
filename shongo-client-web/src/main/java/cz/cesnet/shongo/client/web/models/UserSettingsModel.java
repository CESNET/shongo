package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.UserSettings;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import org.joda.time.DateTimeZone;

import javax.servlet.http.HttpServletRequest;
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
    public final static String SLOT_BEFORE_ATTRIBUTE = "client-web.slot.before";
    public final static String SLOT_AFTER_ATTRIBUTE = "client-web.slot.after";

    /**
     * @see UserSettings
     */
    private UserSettings userSettings;

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
     * @see UserSettings#administrationMode
     */
    private boolean administrationMode;

    /**
     * @see {@link #IGNORE_DEFAULT_HOME_TIME_ZONE_ATTRIBUTE}
     */
    private UserInterface userInterface;

    /**
     * Specifies whether {@link #userInterface} has been selected (and thus question should not be displayed).
     */
    private boolean userInterfaceSelected;

    /**
     * Specifies {@link ReservationRequestModel#slotBeforeMinutes}.
     */
    private Integer slotBefore;

    /**
     * Specifies {@link ReservationRequestModel#slotAfterMinutes}.
     */
    private Integer slotAfter;

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
     * @return {@link #administrationMode}
     */
    public boolean isAdministrationMode()
    {
        return administrationMode;
    }

    /**
     * @param administrationMode sets the {@link #administrationMode}
     */
    public void setAdministrationMode(boolean administrationMode)
    {
        this.administrationMode = administrationMode;
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
     * @return {@link #slotBefore}
     */
    public Integer getSlotBefore()
    {
        return slotBefore;
    }

    /**
     * @param slotBefore sets the {@link #slotBefore}
     */
    public void setSlotBefore(Integer slotBefore)
    {
        this.slotBefore = slotBefore;
    }

    /**
     * @return {@link #slotAfter}
     */
    public Integer getSlotAfter()
    {
        return slotAfter;
    }

    /**
     * @param slotAfter sets the {@link #slotAfter}
     */
    public void setSlotAfter(Integer slotAfter)
    {
        this.slotAfter = slotAfter;
    }

    /**
     * @param userSettings to load this {@link UserSettingsModel} from
     */
    public void fromApi(UserSettings userSettings)
    {
        this.userSettings = userSettings;
        this.useWebService = userSettings.isUseWebService();
        this.locale = userSettings.getLocale();
        this.localeDefaultWarning = !userSettings.getAttributeBool(IGNORE_DEFAULT_LOCALE_ATTRIBUTE);
        this.homeTimeZone = userSettings.getHomeTimeZone();
        this.timeZoneDefaultWarning = !userSettings.getAttributeBool(IGNORE_DEFAULT_HOME_TIME_ZONE_ATTRIBUTE);
        this.currentTimeZone = userSettings.getCurrentTimeZone();
        this.currentTimeZoneEnabled = (this.currentTimeZone != null && !this.currentTimeZone.equals(this.homeTimeZone));
        this.administrationMode = userSettings.getAdministrationMode();

        UserInterface userInterface = userSettings.getAttribute(USER_INTERFACE_ATTRIBUTE, UserInterface.class);
        if (userInterface != null) {
            this.userInterface = userInterface;
        }
        else {
            this.userInterface = DEFAULT_USER_INTERFACE;
        }
        this.userInterfaceSelected = userSettings.getAttributeBool(USER_INTERFACE_SELECTED_ATTRIBUTE);
        this.slotBefore = userSettings.getAttributeInteger(SLOT_BEFORE_ATTRIBUTE);
        this.slotAfter = userSettings.getAttributeInteger(SLOT_AFTER_ATTRIBUTE);
    }

    /**
     * @return {@link UserSettings} from this {@link UserSettingsModel}
     */
    public UserSettings toApi()
    {
        userSettings.setUseWebService(useWebService);
        if (!useWebService) {
            userSettings.setLocale(locale);
            userSettings.setHomeTimeZone(homeTimeZone);
        }
        if (currentTimeZoneEnabled) {
            userSettings.setCurrentTimeZone(currentTimeZone);
        }
        userSettings.setAdministrationMode(administrationMode);
        UserInterface userInterface = getUserInterface();
        if (userInterface != null) {
            userSettings.setAttribute(USER_INTERFACE_ATTRIBUTE, userInterface.toString());
        }
        else {
            userSettings.removeAttribute(USER_INTERFACE_ATTRIBUTE);
        }
        if (userInterfaceSelected) {
            userSettings.setAttribute(USER_INTERFACE_SELECTED_ATTRIBUTE, Boolean.TRUE.toString());
        }
        else {
            userSettings.removeAttribute(USER_INTERFACE_SELECTED_ATTRIBUTE);
        }
        if (isLocaleDefaultWarning()) {
            userSettings.removeAttribute(IGNORE_DEFAULT_LOCALE_ATTRIBUTE);
        }
        else {
            userSettings.setAttribute(IGNORE_DEFAULT_LOCALE_ATTRIBUTE, Boolean.TRUE.toString());
        }
        if (isTimeZoneDefaultWarning()) {
            userSettings.removeAttribute(IGNORE_DEFAULT_HOME_TIME_ZONE_ATTRIBUTE);
        }
        else {
            userSettings.setAttribute(IGNORE_DEFAULT_HOME_TIME_ZONE_ATTRIBUTE, Boolean.TRUE.toString());
        }
        if (slotBefore != null) {
            userSettings.setAttribute(SLOT_BEFORE_ATTRIBUTE, slotBefore.toString());
        }
        if (slotAfter != null) {
            userSettings.setAttribute(SLOT_AFTER_ATTRIBUTE, slotAfter.toString());
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
        attributes.put("AdminMode", administrationMode);
        attributes.put("UserInterface", userInterface);
        attributes.put("SlotBefore", slotBefore);
        attributes.put("SlotAfter", slotAfter);
        return ReportModel.formatAttributes(attributes);
    }

    /**
     * Update {@link UserSettings} when {@link UserSettingsModel#slotBefore} or
     * {@link UserSettingsModel#slotAfter} changed.
     *
     * @param securityToken
     * @param reservationRequest
     * @param request
     * @param authorizationService
     */
    public static void updateSlotSettings(SecurityToken securityToken, ReservationRequestModel reservationRequest,
            HttpServletRequest request, AuthorizationService authorizationService)
    {
        UserSession userSession = UserSession.getInstance(request);
        UserSettingsModel userSettings = userSession.getUserSettings();
        Integer slotBefore = reservationRequest.getSlotBeforeMinutes();
        Integer slotAfter = reservationRequest.getSlotAfterMinutes();
        if (!slotBefore.equals(userSettings.getSlotBefore()) || !slotAfter.equals(userSettings.getSlotAfter())) {
            userSettings = new UserSettingsModel(authorizationService.getUserSettings(securityToken));
            userSettings.setSlotBefore(slotBefore);
            userSettings.setSlotAfter(slotAfter);
            authorizationService.updateUserSettings(securityToken, userSettings.toApi());
            userSession.loadUserSettings(userSettings, request, securityToken);
        }
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
