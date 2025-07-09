package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.auth.OpenIDConnectAuthenticationToken;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.UserSettings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Locale;

/**
 * Represents a user session attributes.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserSession implements Serializable
{
    private static Logger logger = LoggerFactory.getLogger(UserSession.class);

    public final static String USER_SESSION_ATTRIBUTE = "SHONGO_USER";

    /**
     * Current session {@link Locale}.
     */
    private Locale locale;

    /**
     * @see UserSettingsModel#localeDefaultWarning
     */
    private boolean localeDefaultWarning = true;

    /**
     * Current session {@link DateTimeZone}.
     */
    private DateTimeZone timeZone;

    /**
     * Home {@link DateTimeZone}.
     */
    private DateTimeZone homeTimeZone;

    /**
     * @see UserSettingsModel#timeZoneDefaultWarning
     */
    private boolean timeZoneDefaultWarning = true;

    /**
     * {@link DateTimeZone} which was detected from client web-browser for current session.
     *
     * We don't want to perform multiple detections for single user session.
     */
    private DateTimeZone detectedTimeZone;

    /**
     * @see UserSettingsModel
     */
    private UserSettingsModel userSettings;

    /**
     * Number of automatic login attempts.
     */
    private int loginCounter = 0;

    /**
     * Constructor.
     */
    public UserSession()
    {
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
     * @return {@link #timeZone}
     */
    public DateTimeZone getTimeZone()
    {
        return timeZone;
    }

    /**
     * @return {@link Duration} between {@link #timeZone} and {@link #homeTimeZone}
     */
    public Duration getTimeZoneOffset()
    {
        if (homeTimeZone != null && timeZone != null) {
            DateTime dateTime = DateTime.now();
            return Duration.millis(timeZone.getOffset(dateTime) - homeTimeZone.getOffset(dateTime));
        }
        else {
            return null;
        }
    }

    /**
     * @param timeZone sets the {@link #timeZone}
     */
    public void setTimeZone(DateTimeZone timeZone)
    {
        this.timeZone = timeZone;
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
     * @return {@link #timeZoneDefaultWarning}
     */
    public boolean isTimeZoneDefaultWarning()
    {
        return timeZoneDefaultWarning;
    }

    /**
     * @return {@link #detectedTimeZone}
     */
    public DateTimeZone getDetectedTimeZone()
    {
        return detectedTimeZone;
    }

    /**
     * @param detectedTimeZone sets the {@link #detectedTimeZone}
     */
    public void setDetectedTimeZone(DateTimeZone detectedTimeZone)
    {
        this.detectedTimeZone = detectedTimeZone;
    }

    /**
     * @return {@link #userSettings}
     */
    public UserSettingsModel getUserSettings()
    {
        return userSettings;
    }

    /**
     * @return {@link UserSettingsModel#isAdministrationMode()}
     */
    public boolean isAdministrationMode()
    {
        return userSettings != null && userSettings.isAdministrationMode();
    }

    /**
     * @return {@link UserSettingsModel#getUserInterface()}
     */
    public UserSettingsModel.UserInterface getUserInterface()
    {
        if (userSettings != null) {
            return userSettings.getUserInterface();
        }
        else {
            return UserSettingsModel.DEFAULT_USER_INTERFACE;
        }
    }

    /**
     * @return {@link UserSettingsModel#isUserInterfaceSelected()}
     */
    public boolean isUserInterfaceSelected()
    {
        return userSettings != null && userSettings.isUserInterfaceSelected();
    }

    /**
     * @return true whether {@link #getUserInterface()} is {@link UserSettingsModel.UserInterface#ADVANCED},
     *         false otherwise
     */
    public boolean isAdvancedUserInterface()
    {
        return getUserInterface().equals(UserSettingsModel.UserInterface.ADVANCED);
    }

    /**
     * @param request
     * @return {@link UserSession} for user requesting the {@code request}
     */
    public static UserSession getInstance(HttpServletRequest request)
    {
        UserSession userSession = (UserSession) WebUtils.getSessionAttribute(request, USER_SESSION_ATTRIBUTE);
        if (userSession == null) {
            userSession = new UserSession();
            WebUtils.setSessionAttribute(request, USER_SESSION_ATTRIBUTE, userSession);
        }
        return userSession;
    }

    /**
     * Load {@link UserSettings} to this {@link UserSession}.
     *
     * @param userSettings to be loaded
     * @param request      to be used for loading
     * @param token        to be used for loading
     */
    public void loadUserSettings(UserSettingsModel userSettings, HttpServletRequest request, SecurityToken token)
    {
        this.userSettings = userSettings;

        Locale locale = userSettings.getLocale();
        if (locale != null) {
            setLocale(locale);
            localeDefaultWarning = false;
        }
        else {
            localeDefaultWarning = userSettings.isLocaleDefaultWarning();
        }

        // Set timezone from user settings
        homeTimeZone = userSettings.getHomeTimeZone();
        DateTimeZone timeZone = (userSettings.isCurrentTimeZoneEnabled() ? userSettings.getCurrentTimeZone() : null);
        if (timeZone == null) {
            timeZone = homeTimeZone;
        }
        if (timeZone != null) {
            setTimeZone(timeZone);
            timeZoneDefaultWarning = false;
        }
        else {
            setTimeZone(null);
            timeZoneDefaultWarning = userSettings.isTimeZoneDefaultWarning();
        }

        // When timezone should be detected and the detection is already done, use the already detected timezone
        if (this.timeZone == null && detectedTimeZone != null) {
            this.timeZone = detectedTimeZone;
        }
        if (this.homeTimeZone == null && detectedTimeZone != null) {
            this.homeTimeZone = detectedTimeZone;
        }

        update(request, token.getUserInformation());
    }

    /**
     * Store changes to given {@code request}.
     *
     * @param request
     * @param userInformation
     */
    public void update(HttpServletRequest request, UserInformation userInformation)
    {
        if (userInformation == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof OpenIDConnectAuthenticationToken) {
                userInformation = ((OpenIDConnectAuthenticationToken) authentication).getPrincipal();
            }
        }

        logger.debug("Setting (locale: {}, timezone: {}, admin: {}, ui: {}) for {}...", new Object[]{
                locale, timeZone, isAdministrationMode(), getUserInterface(),
                (userInformation != null ? userInformation : "anonymous")
        });

        WebUtils.setSessionAttribute(request, USER_SESSION_ATTRIBUTE, this);
    }

    /**
     * @return true whether automatic login is allowed, false otherwise
     */
    public boolean attemptLogin()
    {
        loginCounter++;
        if (loginCounter < 3) {
            return true;
        }
        return false;
    }

    /**
     * Call when user is logged in.
     */
    public void afterLogin()
    {
        loginCounter = 0;
    }
}
