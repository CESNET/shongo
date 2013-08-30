package cz.cesnet.shongo.controller.authorization;

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
     * Specifies whether user should act in administrator role (for active session). {@code null} means that user isn't
     * administrator.
     */
    private Boolean adminMode;

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
     * @return {@link #adminMode}
     */
    public Boolean getAdminMode()
    {
        return adminMode;
    }

    /**
     * @param adminMode sets the {@link #adminMode}
     */
    public void setAdminMode(Boolean adminMode)
    {
        this.adminMode = adminMode;
    }
}
