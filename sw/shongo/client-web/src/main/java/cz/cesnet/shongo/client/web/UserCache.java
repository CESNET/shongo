package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import org.joda.time.Duration;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.annotation.Resources;

/**
 * Cache of {@link UserInformation}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Component
public class UserCache
{
    @Resource
    private AuthorizationService authorizationService;

    /**
     * {@link UserInformation}s by user-ids.
     */
    private ExpirationMap<String, UserInformation> userInformationById = new ExpirationMap<String, UserInformation>();

    /**
     * Constructor.
     */
    public UserCache()
    {
        userInformationById.setExpiration(Duration.standardMinutes(5));
    }

    /**
     * @param securityToken to be used for fetching the {@link UserInformation}
     * @param userId        user-id of the requested user
     * @return {@link UserInformation} for given {@code userId}
     */
    public synchronized UserInformation getUserInformation(SecurityToken securityToken, String userId)
    {
        UserInformation userInformation = userInformationById.get(userId);
        if (userInformation == null) {
            userInformation = authorizationService.getUser(securityToken, userId);
            userInformationById.put(userId, userInformation);
        }
        return userInformation;
    }
}
