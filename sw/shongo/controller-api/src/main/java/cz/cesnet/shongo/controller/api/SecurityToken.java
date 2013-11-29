package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AtomicType;
import cz.cesnet.shongo.api.UserInformation;

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
    private UserInformation userInformation;

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
     * {@link #userInformation#getUserId()}
     */
    public String getUserId()
    {
        if (userInformation == null) {
            return null;
        }
        return userInformation.getUserId();
    }

    /**
     * @return {@link #userInformation}
     */
    public UserInformation getUserInformation()
    {
        return userInformation;
    }

    /**
     * @param userInformation sets the {@link #userInformation}
     */
    public void setUserInformation(UserInformation userInformation)
    {
        this.userInformation = userInformation;
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

    @Override
    public boolean equals(Object object)
    {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        SecurityToken that = (SecurityToken) object;
        if (!accessToken.equals(that.accessToken)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        return accessToken.hashCode();
    }
}
