package cz.cesnet.shongo.controller.domains;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class BasicAuthFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(InterDomainAgent.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest sr, ServletResponse ss, FilterChain chain) throws IOException, ServletException {
//        HttpServletResponse res = (HttpServletResponse) ss;
//        sr.
//        if (certs == null || certs.length == 0) {
//            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Client authentication required !");
//            return;
//        }
//        if (!checkAllowedCert(certs[0])) {
//            logger.debug("Client cert %s declined", certs[0].toString());
//            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Client authentication required !");
//            return;
//        }
        chain.doFilter(sr, ss);
    }

//    private boolean checkAllowedCert(String domain) {
//        try {
//            return (InterDomainAgent.getInstance().getDomain(cert) != null);
//        } catch (IllegalArgumentException e) {
//            logger.error("InterDomainAgent has not started yet.", e);
//            return false;
//        }
//    }

    @Override
    public void destroy() {
    }
}
