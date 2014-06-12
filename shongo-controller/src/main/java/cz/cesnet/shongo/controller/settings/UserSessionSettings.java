package cz.cesnet.shongo.controller.settings;

import cz.cesnet.shongo.controller.api.SecurityToken;

/**
 * Represents a user settings for a single session.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserSessionSettings
{
    /**
     * {@link SecurityToken} of an user to who the {@link UserSessionSettings} belongs.
     */
    private final SecurityToken securityToken;

    /**
     * Specifies whether user should act as administrator for active session (i.e., he can see all reservation requests).
     */
    private boolean administrationMode = false;

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public UserSessionSettings(SecurityToken securityToken)
    {
        this.securityToken = securityToken;
    }

    /**
     * @return {@link #securityToken}
     */
    public SecurityToken getSecurityToken()
    {
        return securityToken;
    }

    /**
     * @return {@link #administrationMode}
     */
    public boolean getAdministrationMode()
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
}
