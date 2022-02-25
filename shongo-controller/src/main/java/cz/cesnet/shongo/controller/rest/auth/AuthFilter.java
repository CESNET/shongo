package cz.cesnet.shongo.controller.rest.auth;

import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.authorization.Authorization;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Authorizes request based on authentication token.
 *
 * @author Filip Karnis
 */
public class AuthFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ControllerReportSet.SecurityInvalidTokenException, ServletException, IOException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Authorization authorization = Authorization.getInstance();

        String accessToken = httpRequest.getHeader("Authorization");
        if (accessToken == null) {
            httpResponse.sendError(401, "No Authorization header found.");
            return;
        }

        String[] tokenParts = accessToken.split("Bearer");
        if (tokenParts.length != 2) {
            httpResponse.sendError(401, "Invalid access token.");
            return;
        }

        String sanitizedToken = accessToken.split("Bearer")[1].strip();
        SecurityToken securityToken = new SecurityToken(sanitizedToken);
        securityToken.setUserInformation(authorization.getUserInformation(securityToken));

        try {
            authorization.validate(securityToken);
        } catch (ControllerReportSet.SecurityInvalidTokenException e) {
            httpResponse.sendError(401, "Request unauthorized.");
            return;
        }
        chain.doFilter(httpRequest, httpResponse);
    }
}
