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
import java.io.IOException;
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

    public final static String USER_SESSION_ATTRIBUTE = "user";

    /**
     * Current session {@link Locale}.
     */
    private Locale locale;

    /**
     * Current session {@link DateTimeZone}.
     */
    private DateTimeZone timeZone;

    /**
     * Specifies whether current session is in {@link UserSettings#adminMode}.
     */
    private boolean admin;

    /**
     * Constructor.
     */
    public UserSession()
    {
        this.locale = null;
        this.timeZone = null;
        this.admin = false;
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
     * @return {@link #admin}
     */
    public boolean isAdmin()
    {
        return admin;
    }

    /**
     * @param admin sets the {@link #admin}
     */
    public void setAdmin(boolean admin)
    {
        this.admin = admin;
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
    public void loadUserSettings(UserSettings userSettings, HttpServletRequest request, SecurityToken securityToken)
    {
        // Set locale
        Locale locale;
        if (userSettings.getLocale() != null) {
            locale = userSettings.getLocale();
        }
        else {
            locale = request.getLocale();
        }
        setLocale(locale);
        setTimeZone(userSettings.getTimeZone());
        setAdmin(userSettings.getAdminMode());
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

        logger.info("Setting (locale: {}, timezone: {}, admin: {}) for {}...", new Object[]{
                locale, timeZone, admin, (userInformation != null ? userInformation : "anonymous")
        });

        WebUtils.setSessionAttribute(request, USER_SESSION_ATTRIBUTE, this);
    }
}
