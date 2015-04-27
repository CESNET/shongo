package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.controller.ForeignDomainConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class SSLClientCertFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(InterDomainAgent.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest sr, ServletResponse ss, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse res = (HttpServletResponse) ss;
        //viz https://java.net/downloads/servlet-spec/Final/servlet-3_1-final-change-bar.pdf part 3.9
        X509Certificate[] certs = (X509Certificate[]) sr.getAttribute("javax.servlet.request.X509Certificate");
        if (certs == null || certs.length == 0) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Client authentication required !");
            return;
        }
        if (!checkAllowedCert(certs[0])) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Client authentication required !");
            return;
        }
        chain.doFilter(sr, ss);
    }

    private boolean checkAllowedCert(X509Certificate cert) {
        try {
        for (X509Certificate allowedCert : InterDomainAgent.getInstance().listForeignDomainCertificates()) {
            if(cert.equals(allowedCert)) return true;
        }
        } catch (IllegalArgumentException e) {
            logger.error("InterDomainAgent has not started yet.", e);
            return false;
        }
        return false;
    }

    static private X509Certificate readPEMCert(String certFile) throws CertificateException, IOException {
        CertificateFactory certFact = CertificateFactory.getInstance("X.509");
        File cf = new File(certFile);
        byte[] b = new byte[(int) cf.length()];
        FileInputStream fis = new FileInputStream(cf);
        //noinspection ResultOfMethodCallIgnored
        fis.read(b, 0, b.length);
        String s = new String(b);
        int z1 = s.indexOf("-----BEGIN CERTIFICATE-----");
        String END = "-----END CERTIFICATE-----";
        int z2 = s.indexOf(END, z1);
        String c = s.substring(z1, z2 + END.length());
        ByteArrayInputStream bain = new ByteArrayInputStream(c.getBytes());
        return (X509Certificate) certFact.generateCertificate(bain);
    }


    @Override
    public void destroy() {
    }
}