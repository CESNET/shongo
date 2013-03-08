import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Example demonstrating consequences of jsse.enableSNIExtension.
 * Output of the {@code run.sh} script:
 * <pre>
 *
 *     Java 1.6 Oracle
 *     SNI Extension On
 *     Certificate from shongo-auth-dev.cesnet.cz alternative names [ hroch.cesnet.cz ]
 *     SNI Extension Off
 *     Certificate from shongo-auth-dev.cesnet.cz alternative names [ hroch.cesnet.cz ]
 *
 *     Java 1.7 Oracle
 *     SNI Extension On
 *     Certificate from shongo-auth-dev.cesnet.cz alternative names [ shongo-auth.cesnet.cz shongo-auth-dev.cesnet.cz ]
 *     SNI Extension Off
 *     Certificate from shongo-auth-dev.cesnet.cz alternative names [ hroch.cesnet.cz ]
 *
 * </pre>
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SslSniExtension
{
    public static void main(String[] args) throws Exception
    {
        String host = "shongo-auth-dev.cesnet.cz";

        if (args.length >= 1 && args[0].equals("sni_extension_off")) {
            System.out.println("SNI Extension Off");
            System.setProperty("jsse.enableSNIExtension", "false");
        }
        else {
            System.out.println("SNI Extension On");
            System.setProperty("jsse.enableSNIExtension", "true");
        }

        javax.net.ssl.SSLContext tempConnectContext = javax.net.ssl.SSLContext.getInstance("TLS");
        ExtractX509CertTrustManager x509CertTrustManager = new ExtractX509CertTrustManager();
        tempConnectContext.init(null, new javax.net.ssl.TrustManager[]{x509CertTrustManager}, null);

        javax.net.ssl.SSLSocketFactory socketFactory = tempConnectContext.getSocketFactory();
        javax.net.ssl.SSLSocket socket = (javax.net.ssl.SSLSocket) socketFactory.createSocket(host, 443);
        socket.startHandshake();

        for (X509Certificate cert : x509CertTrustManager.currentChain) {
            if (cert.getSubjectAlternativeNames() != null) {
                System.out.print("Certificate from " + host + " alternative names [ ");
                for (List item : cert.getSubjectAlternativeNames()) {
                    System.out.print(item.get(1).toString() + " ");
                }
                System.out.println("]");
            }
        }
    }

    private static class ExtractX509CertTrustManager implements javax.net.ssl.X509TrustManager
    {
        private X509Certificate[] currentChain;

        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException
        {
        }

        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException
        {
            currentChain = x509Certificates;
        }

        public X509Certificate[] getAcceptedIssuers()
        {
            return null;
        }
    }
}
