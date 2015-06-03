package cz.cesnet.shongo.ssl;

import org.apache.ws.commons.util.Base64;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.*;

/**
 * Makes HTTPS connection.
 * <ul>
 *     <li>supports multiple client certificates</li>
 *     <li>supports own set of root CAs</li>
 * </ul>
 *
 * @author Martin Kuba <makub@ics.muni.cz>, Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class SSLCommunication {

    /**
     * Reads X509 certificate from a PEM encoded file.
     * @param certFile file
     * @return certificate
     * @throws CertificateException
     * @throws IOException
     */
    public static X509Certificate readPEMCert(String certFile) throws CertificateException, IOException {
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

    /**
     * Returns decoded credentials from basic auth header
     * @param request
     * @return
     */
    public static String[] getBasicAuthCredentials(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if ((authHeader == null || !authHeader.startsWith("Basic "))) {
                throw new BasicAuthException("Basic header not found.");
            }
            String enc = authHeader.substring(authHeader.indexOf(" ") + 1);
            byte[] bytes = new byte[0];
            bytes = Base64.decode(enc);
            String s = new String(bytes);
            int pos = s.indexOf(":");
            if (pos >= 0) {
                return new String[]{s.substring(0, pos), s.substring(pos + 1)};
            }
            else {
                return new String[]{s, null};
            }
        } catch (Base64.DecodingException e) {
            throw new BasicAuthException("Failed to decode basic authentication header.", e);
        }
    }

    public static String hashPassword(byte[] password) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(password);
            byte[] bytes = messageDigest.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        }
        catch (NoSuchAlgorithmException e) {
            String message = "Failed to get messageDigest.";
            throw new RuntimeException(message, e);
        }
    }

    public static class BasicAuthException extends RuntimeException {
        public BasicAuthException(String message)
        {
            super(message);
        }

        public BasicAuthException(String message, Throwable e)
        {
            super(message, e);
        }
    }
}