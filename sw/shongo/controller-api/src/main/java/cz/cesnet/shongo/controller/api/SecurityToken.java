package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.rpc.AtomicType;

/**
 * Represents a security token by which the user tells his identity.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SecurityToken implements AtomicType
{
    /**
     * Access token of the user which is accessing Shongo {@link Controller}.
     */
    private String accessToken;

    /**
     * Cached person information of the user.
     */
    private UserInformation cachedUserInformation;

    /**
     * Constructor.
     */
    public SecurityToken()
    {
    }

    /**
     * Constructor.
     *
     * @param accessToken sets the {@link #accessToken}
     */
    public SecurityToken(String accessToken)
    {
        this.accessToken = accessToken;
    }

    /**
     * @return {@link #accessToken}
     */
    public String getAccessToken()
    {
        return accessToken;
    }

    /**
     * @return {@link #cachedUserInformation}
     */
    public UserInformation getCachedUserInformation()
    {
        return cachedUserInformation;
    }

    /**
     * @param cachedUserInformation sets the {@link #cachedUserInformation}
     */
    public void setCachedUserInformation(UserInformation cachedUserInformation)
    {
        this.cachedUserInformation = cachedUserInformation;
    }

    @Override
    public void fromString(String string)
    {
        accessToken = string;
    }

    @Override
    public String toString()
    {
        return accessToken;
    }
}
