package cz.cesnet.shongo.connector.storage;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;

/**
 * Abstract {@link Storage} implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractStorage implements Storage
{
    /**
     * URL of the {@link AbstractStorage}.
     */
    private String url;

    /**
     * @see UserInformationProvider
     */
    private UserInformationProvider userInformationProvider;

    /**
     * Constructor.
     *
     * @param url sets the {@link #url}
     * @param userInformationProvider sets the {@link #userInformationProvider}
     */
    protected AbstractStorage(String url, UserInformationProvider userInformationProvider)
    {
        this.url = url;
        this.userInformationProvider = userInformationProvider;
    }

    /**
     * @return {@link #url}
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * @param userId of user
     * @return {@link UserInformation} for user with given {@code userId}
     */
    protected UserInformation getUserInformation(String userId)
    {
        return userInformationProvider.getUserInformation(userId);
    }

    /**
     * Provider for {@link UserInformation}s by user-ids.
     */
    public static interface UserInformationProvider
    {
        /**
         * @param userId of user
         * @return {@link UserInformation} for user with given {@code userId}
         */
        public UserInformation getUserInformation(String userId);
    }
}
