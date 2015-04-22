package cz.cesnet.shongo.controller.domains;

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

@WebFilter(urlPatterns = {"/*"})
public class SSLClientCertFilter implements Filter {

    List<X509Certificate> allowedCerts = new ArrayList<X509Certificate>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            allowedCerts.add(readPEMCert("keystore/localhostREST.crt"));
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        for (X509Certificate allowedCert : allowedCerts) {
            if(cert.equals(allowedCert)) return true;
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