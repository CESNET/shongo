package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.api.UserInformation;

import java.util.Locale;

/**
 * Represents user data fetched from web service.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserData
{
    /**
     * @see cz.cesnet.shongo.api.UserInformation
     */
    private final UserInformation userInformation = new UserInformation();

    /**
     * Use preferred language.
     */
    private Locale locale;

    /**
     * User authorization data.
     */
    private UserAuthorizationData userAuthorizationData;

    /**
     * @return {@link #userInformation}
     */
    public UserInformation getUserInformation()
    {
        return userInformation;
    }

    /**
     * @return {@link #userInformation#getUserId()}
     */
    public String getUserId()
    {
        return userInformation.getUserId();
    }

    /**
     * @return {@link #userInformation#getFullName()} ()}
     */
    public String getFullName()
    {
        return userInformation.getFullName();
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
     * @return {@link #userAuthorizationData}
     */
    public UserAuthorizationData getUserAuthorizationData()
    {
        return userAuthorizationData;
    }

    /**
     * @param userAuthorizationData sets the {@link #userAuthorizationData}
     */
    public void setUserAuthorizationData(UserAuthorizationData userAuthorizationData)
    {
        this.userAuthorizationData = userAuthorizationData;
    }
}
