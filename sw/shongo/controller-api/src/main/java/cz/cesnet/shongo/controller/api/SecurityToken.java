package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.PersonInformation;
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
    private PersonInformation cachedPersonInformation;

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
     * @return {@link #cachedPersonInformation}
     */
    public PersonInformation getCachedPersonInformation()
    {
        return cachedPersonInformation;
    }

    /**
     * @param cachedPersonInformation sets the {@link #cachedPersonInformation}
     */
    public void setCachedPersonInformation(PersonInformation cachedPersonInformation)
    {
        this.cachedPersonInformation = cachedPersonInformation;
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
