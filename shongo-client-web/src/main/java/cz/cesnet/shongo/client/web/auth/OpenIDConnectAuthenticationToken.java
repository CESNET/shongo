package cz.cesnet.shongo.client.web.auth;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.SecurityToken;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Authentication token for OpenID Connect.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class OpenIDConnectAuthenticationToken extends AbstractAuthenticationToken
{
    private SecurityToken securityToken;

    private UserInformation userInformation;

    public OpenIDConnectAuthenticationToken(String accessToken)
    {
        super(new LinkedList<GrantedAuthority>());
        this.securityToken = new SecurityToken(accessToken);
    }

    public OpenIDConnectAuthenticationToken(SecurityToken securityToken, UserInformation userInformation)
    {
        super(new LinkedList<GrantedAuthority>());
        this.securityToken = securityToken;
        this.userInformation = userInformation;
    }

    public SecurityToken getSecurityToken()
    {
        return securityToken;
    }

    @Override
    public UserInformation getPrincipal()
    {
        return userInformation;
    }

    @Override
    public Object getCredentials()
    {
        return null;
    }
}
