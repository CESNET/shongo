package cz.cesnet.shongo.controller.domains;

import com.google.common.base.Strings;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.domains.InterDomainAction;
import cz.cesnet.shongo.ssl.SSLCommunication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class BasicAuthFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(InterDomainAgent.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest sr, ServletResponse ss, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse res = (HttpServletResponse) ss;
        try {
            HttpServletRequest req = (HttpServletRequest) sr;
            String[] credentials = SSLCommunication.getBasicAuthCredentials(req);
            if (InterDomainAction.DOMAIN_LOGIN.equals(req.getPathInfo())) {
                String domainCode = credentials[0];
                String password = credentials[1];
                if (Strings.isNullOrEmpty(domainCode) || Strings.isNullOrEmpty(password) || !checkAllowedDomain(domainCode, password)) {
                    logger.debug("Client credentials declined for domain ", domainCode);
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Client authentication required !");
                    return;
                }
            }
            else {
                String accessToken = credentials[0];
                if (Strings.isNullOrEmpty(accessToken) || !checkAccessToken(accessToken)) {
                    logger.debug("Client access token \"" + accessToken + "\" was declined .");
                    res.sendError(HttpServletResponse.SC_FORBIDDEN, "Client access token was rejected !");
                    return;
                }
            }
        }
        catch (SSLCommunication.BasicAuthException e) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Client authentication required !");
            return;
        }
        chain.doFilter(sr, ss);
    }

    private boolean checkAllowedDomain(String domainCode, String password) {
        try {
            Domain domain = getDomainService().findDomainByCode(domainCode);
            if (domain == null || Strings.isNullOrEmpty(password)) {
                return false;
            }
            if (password.equals(domain.getPasswordHash())) {
                return true;
            }
        } catch (IllegalArgumentException e) {
            logger.error("InterDomainAgent has not started yet.", e);
            return false;
        }
        return false;
    }

    private boolean checkAccessToken(String accessToken) {
        try {
            Domain domain = getAuthentication().getDomain(accessToken);
            if (domain != null) {
                return true;
            }
        } catch (IllegalArgumentException e) {
            logger.error("InterDomainAgent has not started yet.", e);
            return false;
        }
        return false;    }

    @Override
    public void destroy() {
    }

    protected DomainService getDomainService() {
        return InterDomainAgent.getInstance().getDomainService();
    }

    protected DomainAuthentication getAuthentication() {
        return InterDomainAgent.getInstance().getAuthentication();
    }
}
