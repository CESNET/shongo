package cz.cesnet.shongo.client.web.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.LinkedList;

/**
 * Authentication provider.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class OpenIDConnectAuthenticationProvider
        implements org.springframework.security.authentication.AuthenticationProvider
{
    private static Logger logger = LoggerFactory.getLogger(OpenIDConnectAuthenticationProvider.class);

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException
    {
        logger.info("Authenticating...");
        return new OpenIDConnectAuthenticationToken("test", new LinkedList<GrantedAuthority>()
        {{
                add(new SimpleGrantedAuthority("ROLE_USER"));
            }});
    }

    @Override
    public boolean supports(Class<?> authentication)
    {
        return OpenIDConnectAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
