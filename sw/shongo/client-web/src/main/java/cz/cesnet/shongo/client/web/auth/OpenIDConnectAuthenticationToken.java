package cz.cesnet.shongo.client.web.auth;

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
    private String accessToken;

    private UserInfo userInformation;

    public OpenIDConnectAuthenticationToken(String accessToken)
    {
        super(new LinkedList<GrantedAuthority>());
        this.accessToken = accessToken;
    }

    public OpenIDConnectAuthenticationToken(String accessToken, UserInfo userInformation,
            Collection<? extends GrantedAuthority> authorities)
    {
        super(authorities);
        this.accessToken = accessToken;
        this.userInformation = userInformation;
    }

    public String getAccessToken()
    {
        return accessToken;
    }

    @Override
    public UserInfo getPrincipal()
    {
        return userInformation;
    }

    @Override
    public Object getCredentials()
    {
        return null;
    }
}
