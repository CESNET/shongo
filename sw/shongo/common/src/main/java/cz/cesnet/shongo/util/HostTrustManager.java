package cz.cesnet.shongo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

/**
 * SSL {@link TrustManager} for trusting hosts.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class HostTrustManager implements X509TrustManager
{
    private static Logger logger = LoggerFactory.getLogger(HostTrustManager.class);

    /**
     * SSL and certificates constants.
     */
    private static final String JAVA_CA_CERT_FILE_NAME = "cacerts";
    private static final String CLASSIC_JAVA_CA_CERT_FILE_NAME = "jssecacerts";
    private static final int DEFAULT_HTTPS_PORT = 443;

    /**
     * Set of hosts which should be trusted.
     */
    private Set<String> hostsToTrust = new HashSet<String>();

    /**
     * Store of certificates.
     */
    private KeyStore certificateTrustStore;

    /**
     * Default trust manager.
     */
    private X509TrustManager defaultTrustManager;

    /**
     * Single instance of {@link HostTrustManager}.
     */
    private static HostTrustManager trustManager;

    /**
     * @return {@link #trustManager}
     */
    public static synchronized HostTrustManager getInstance()
    {
        if (trustManager == null) {
            try {
                // Create trust manager
                trustManager = new HostTrustManager();

                // Set it to the SSL context
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, new TrustManager[]{trustManager}, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier()
                {
                    public boolean verify(String arg0, SSLSession arg1)
                    {
                        // Host names should be equal
                        return arg1.getPeerHost().equals(arg0);
                        // We don't want to always return true
                        // return true;
                    }
                });
            }
            catch (Exception exception) {
                throw new IllegalStateException(exception);
            }

        }
        return trustManager;
    }

    /**
     * Constructor.
     *
     * @throws Exception
     */
    public HostTrustManager() throws Exception
    {
        File javaTrustStoreFile = findJavaTrustStoreFile();
        InputStream inputStream = new FileInputStream(javaTrustStoreFile);
        certificateTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        certificateTrustStore.load(inputStream, null);
        inputStream.close();
        initDefaultTrustManager();
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
    {
        defaultTrustManager.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
    {
        defaultTrustManager.checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers()
    {
        return defaultTrustManager.getAcceptedIssuers();
    }

    /**
     * @param host to be added as trusted to the {@link HostTrustManager}
     */
    public synchronized void addTrustedHost(String host)
    {
        if (hostsToTrust.add(host)) {
            try {
                SSLContext tempConnectContext = SSLContext.getInstance("TLS");
                ExtractX509CertTrustManager x509CertTrustManager = new ExtractX509CertTrustManager();
                tempConnectContext.init(null, new TrustManager[]{x509CertTrustManager}, null);
                SSLSocketFactory socketFactory = tempConnectContext.getSocketFactory();
                SSLSocket socket = (SSLSocket) socketFactory.createSocket(host, DEFAULT_HTTPS_PORT);
                socket.setSoTimeout(10000);
                socket.startHandshake();
                for (X509Certificate cert : x509CertTrustManager.getCurrentChain()) {
                    if (!certificateTrustStore.isCertificateEntry(host)) {
                        certificateTrustStore.setCertificateEntry(host, cert);
                    }
                }
                initDefaultTrustManager();
                logger.info("Host '{}' has been added to the list of trusted hosts.", host);
            }
            catch (Exception exception) {
                logger.error(String.format("Host '%s' failed to be added to the list of trusted hosts.", host),
                        exception);
            }
        }
    }

    /**
     * (Re-)initialize {@link #defaultTrustManager}.
     *
     * @throws Exception
     */
    private void initDefaultTrustManager() throws Exception
    {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(certificateTrustStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager) {
                defaultTrustManager = (X509TrustManager) trustManager;
                break;
            }
        }
    }

    /**
     * Trust Manager for the sole purpose of retrieving the X509 cert when a connection is made to a host we want
     * to start trusting.
     */
    private static class ExtractX509CertTrustManager implements X509TrustManager
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

        public X509Certificate[] getCurrentChain()
        {
            return currentChain;
        }
    }

    /**
     * @return {@link File} for trusted certificates
     */
    private static File findJavaTrustStoreFile()
    {
        File javaHome = new File(
                System.getProperty("java.home") + File.separatorChar + "lib" + File.separatorChar + "security");
        File caCertsFile = new File(javaHome, JAVA_CA_CERT_FILE_NAME);
        if (!caCertsFile.exists() || !caCertsFile.isFile()) {
            caCertsFile = new File(javaHome, CLASSIC_JAVA_CA_CERT_FILE_NAME);
        }
        return caCertsFile;
    }
}
