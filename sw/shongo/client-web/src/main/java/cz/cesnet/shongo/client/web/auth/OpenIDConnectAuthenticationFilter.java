package cz.cesnet.shongo.client.web.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;

/**
 * TODO:
 *
 * Based on https://github.com/mitreid-connect/OpenID-Connect-Java-Spring-Server/tree/master/openid-connect-client}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class OpenIDConnectAuthenticationFilter extends AbstractAuthenticationProcessingFilter
{
    private static Logger logger = LoggerFactory.getLogger(OpenIDConnectAuthenticationFilter.class);

    protected final static String FILTER_PROCESSES_URL = "/login";

    protected OpenIDConnectAuthenticationFilter()
    {
        super(FILTER_PROCESSES_URL);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException
    {
        logger.info("Returning token...");
        return new OpenIDConnectAuthenticationToken("test");
    }
}
