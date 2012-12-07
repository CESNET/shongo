package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.xmlrpc.AtomicType;

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
