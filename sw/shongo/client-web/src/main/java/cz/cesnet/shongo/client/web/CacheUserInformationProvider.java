package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.auth.UserInformationProvider;
import cz.cesnet.shongo.controller.api.SecurityToken;

/**
 * {@link cz.cesnet.shongo.client.web.auth.UserInformationProvider} by {@link Cache}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CacheUserInformationProvider implements UserInformationProvider
{
    /**
     * {@link Cache} to be used for retrieving {@link UserInformation}.
     */
    private Cache cache;

    /**
     * {@link SecurityToken} to be used for retrieving {@link UserInformation} by the {@link #cache}.
     */
    private SecurityToken securityToken;

    /**
     * Constructor.
     *
     * @param cache sets the {@link #cache}
     * @param securityToken sets the {@link #securityToken}
     */
    public CacheUserInformationProvider(Cache cache, SecurityToken securityToken)
    {
        this.cache = cache;
        this.securityToken = securityToken;
    }

    @Override
    public UserInformation getUserInformation(String userId)
    {
        return cache.getUserInformation(securityToken, userId);
    }
}
