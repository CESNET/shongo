package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.auth.OpenIDConnectAuthenticationToken;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.UserSettings;
import org.joda.time.DateTimeZone;
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
    private boolean localeDefaultWarning;

    /**
     * Current session {@link DateTimeZone}.
     */
    private DateTimeZone timeZone;

    /**
     * @see UserSettingsModel#timeZoneDefaultWarning
     */
    private boolean timeZoneDefaultWarning;

    /**
     * @see UserSettingsModel#adminMode
     */
    private Boolean adminMode;

    /**
     * @see UserSettingsModel.UserInterface
     */
    private UserSettingsModel.UserInterface userInterface;

    /**
     * Constructor.
     */
    public UserSession()
    {
        this.locale = null;
        this.timeZone = null;
        this.adminMode = null;
        this.userInterface = UserSettingsModel.DEFAULT_USER_INTERFACE;
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
     * @return {@link #userInterface}
     */
    public UserSettingsModel.UserInterface getUserInterface()
    {
        return userInterface;
    }

    /**
     * @return true whether {@link #userInterface} is {@link UserSettingsModel.UserInterface#ADVANCED},
     *         false otherwise
     */
    public boolean isAdvancedUserInterface()
    {
        return userInterface.equals(UserSettingsModel.UserInterface.ADVANCED);
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
     * @param userSettings  to be loaded
     * @param request       to be used for loading
     * @param securityToken to be used for loading
     */
    public void loadUserSettings(UserSettingsModel userSettings, HttpServletRequest request,
            SecurityToken securityToken)
    {
        if (userSettings.getLocale() != null) {
            setLocale(userSettings.getLocale());
            localeDefaultWarning = false;
        }
        else {
            localeDefaultWarning = userSettings.isLocaleDefaultWarning();
        }
        if (userSettings.getTimeZone() != null) {
            setTimeZone(userSettings.getTimeZone());
            timeZoneDefaultWarning = false;
        }
        else {
            timeZoneDefaultWarning = userSettings.isTimeZoneDefaultWarning();
        }
        adminMode = (userSettings.isAdminModeAvailable() ? userSettings.isAdminMode() : null);
        userInterface = userSettings.getUserInterface();

        update(request, securityToken.getUserInformation());
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

        logger.info("Setting (locale: {}, timezone: {}, admin: {}, ui: {}) for {}...", new Object[]{
                locale, timeZone, adminMode, userInterface, (userInformation != null ? userInformation : "anonymous")
        });

        WebUtils.setSessionAttribute(request, USER_SESSION_ATTRIBUTE, this);
    }
}
