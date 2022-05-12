package cz.cesnet.shongo.controller.rest.config.security;

import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.authorization.Authorization;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Authorizes request based on authentication token.
 *
 * @author Filip Karnis
 */
public class AuthFilter extends GenericFilterBean {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER = "Bearer";
    public static final String TOKEN = "TOKEN";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ControllerReportSet.SecurityInvalidTokenException, ServletException, IOException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Authorization authorization = Authorization.getInstance();

        String accessToken = httpRequest.getHeader(AUTHORIZATION_HEADER);
        if (accessToken == null) {
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No Authorization header found.");
            return;
        }

        String[] tokenParts = accessToken.split(BEARER);
        if (tokenParts.length != 2) {
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid access token.");
            return;
        }

        String sanitizedToken = tokenParts[1].strip();
        SecurityToken securityToken = new SecurityToken(sanitizedToken);
        securityToken.setUserInformation(authorization.getUserInformation(securityToken));

        try {
            authorization.validate(securityToken);
            httpRequest.setAttribute(TOKEN, securityToken);
        } catch (ControllerReportSet.SecurityInvalidTokenException e) {
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Request unauthorized.");
            return;
        }
        chain.doFilter(httpRequest, httpResponse);
    }
}
