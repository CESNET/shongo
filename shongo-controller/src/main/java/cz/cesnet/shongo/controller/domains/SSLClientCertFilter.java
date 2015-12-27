package cz.cesnet.shongo.controller.domains;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

public class SSLClientCertFilter implements Filter
{

    private static final Logger logger = LoggerFactory.getLogger(InterDomainAgent.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
    }

    @Override
    public void doFilter(ServletRequest sr, ServletResponse ss, FilterChain chain) throws IOException, ServletException
    {
        HttpServletResponse res = (HttpServletResponse) ss;

        //            ====================DEBUG=====================
//        HttpServletRequest req = (HttpServletRequest) sr;
//        System.out.println("Http headers");
//        Enumeration<String> headerNames = req.getHeaderNames();
//        while (headerNames.hasMoreElements()) {
//            String headerName = headerNames.nextElement();
//            String headerValue = req.getHeader(headerName);
//            System.out.println(headerName + ":" + headerValue);
//        }
//            ====================DEBUG=====================

        //according https://java.net/downloads/servlet-spec/Final/servlet-3_1-final-change-bar.pdf part 3.9
        X509Certificate[] certs = (X509Certificate[]) sr.getAttribute("javax.servlet.request.X509Certificate");
        if (certs == null || certs.length == 0) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Client authentication required !");
            return;
        }
        if (!checkAllowedCert(certs[0])) {
            logger.debug("Client cert %s declined", certs[0].toString());
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Client authentication required !");
            return;
        }
        chain.doFilter(sr, ss);
    }

    private boolean checkAllowedCert(X509Certificate cert)
    {
        try {
            return (InterDomainAgent.getInstance().getAuthentication().getDomain(cert) != null);
        } catch (IllegalArgumentException e) {
            logger.error("InterDomainAgent has not started yet.", e);
            return false;
        }
    }

    @Override
    public void destroy()
    {
    }
}