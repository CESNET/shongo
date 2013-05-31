package cz.cesnet.shongo.client.web.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.LinkedList;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class OpenIDConnectAuthenticationToken extends AbstractAuthenticationToken
{
    private Object principal;

    public OpenIDConnectAuthenticationToken(Object principal)
    {
        super(new LinkedList<GrantedAuthority>());
        this.principal = principal;
    }

    public OpenIDConnectAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities)
    {
        super(authorities);
        this.principal = principal;
    }

    @Override
    public Object getPrincipal()
    {
        return principal;
    }

    @Override
    public Object getCredentials()
    {
        return null;
    }
}
