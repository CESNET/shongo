package cz.cesnet.shongo.client.web.auth;

import cz.cesnet.shongo.api.UserInformation;

/**
 * Interface for retrieving {@link UserInformation} by {@link UserInformation#getUserId()}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface UserInformationProvider
{
    /**
     * @param userId
     * @return {@link UserInformation} for given {@code userId}
     */
    public UserInformation getUserInformation(String userId);
}
