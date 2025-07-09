package cz.cesnet.shongo.client.web.auth;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * The <code>commence</code> method will always return an
 * <code>HttpServletResponse.SC_UNAUTHORIZED</code> (401 error).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Http401UnauthorizedEntryPoint implements AuthenticationEntryPoint
{
    /**
     * Always returns a 401 error code to the client.
     */
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException arg2)
            throws IOException, ServletException
    {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access Denied");
    }


}
