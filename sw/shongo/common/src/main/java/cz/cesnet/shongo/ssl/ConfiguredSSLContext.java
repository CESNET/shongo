package cz.cesnet.shongo.ssl;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AbstractVerifier;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * SSL {@link javax.net.ssl.TrustManager} for trusting hosts.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ConfiguredSSLContext
{
    private static Logger logger = LoggerFactory.getLogger(ConfiguredSSLContext.class);

    private TrustManager trustManager;

    private HostnameVerifier hostnameVerifier;

    private javax.net.ssl.SSLContext context;

    /**
     * Constructor.
     */
    public ConfiguredSSLContext()
    {
        try {
            // Set it to the SSL context
            hostnameVerifier = new HostnameVerifier();
            trustManager = new TrustManager();

            context = javax.net.ssl.SSLContext.getInstance("TLS");
            context.init(null, new javax.net.ssl.TrustManager[]{trustManager}, new SecureRandom());
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    /**
     * @return {@link #trustManager}
     */
    public javax.net.ssl.X509TrustManager getTrustManager()
    {
        return trustManager;
    }

    /**
     * @return {@link #hostnameVerifier}
     */
    public X509HostnameVerifier getHostnameVerifier()
    {
        return hostnameVerifier;
    }

    /**
     * @return {@link #context}
     */
    public javax.net.ssl.SSLContext getContext()
    {
        return context;
    }

    /**
     * @param mappedHost which host should be mapped
     * @param targetHost target host to which the {@code mappedHost} should be mapped
     */
    public void addTrustedHostMapping(String mappedHost, String targetHost)
    {
        hostnameVerifier.addHostMapping(mappedHost, targetHost);
    }

    /**
     * @param host from which should be downloaded all it's certificates and which should be added as trusted
     */
    public void addAdditionalCertificates(String host)
    {
        trustManager.addCertificates(host);
    }

    /**
     * @return new {@link HttpClient} configured with the {@link ConfiguredSSLContext}
     */
    public HttpClient createHttpClient()
    {
        ConfiguredSSLContext configuredSSLContext = getInstance();
        SchemeRegistry registry = new SchemeRegistry();
        org.apache.http.conn.ssl.SSLSocketFactory socketFactory = new org.apache.http.conn.ssl.SSLSocketFactory(
                configuredSSLContext.getContext(), configuredSSLContext.getHostnameVerifier());
        registry.register(new Scheme("https", 443, socketFactory));
        ClientConnectionManager connectionManager = new PoolingClientConnectionManager(registry);
        return new DefaultHttpClient(connectionManager);
    }

    /**
     * Single instance of {@link cz.cesnet.shongo.ssl.ConfiguredSSLContext}.
     */
    private static ConfiguredSSLContext instance;

    /**
     * @return {@link #instance}
     */
    public static synchronized ConfiguredSSLContext getInstance()
    {
        if (instance == null) {
            instance = new ConfiguredSSLContext();
        }
        return instance;
    }

    private class HostnameVerifier extends AbstractVerifier
    {
        /**
         * Set of host mappings which are trusted in the certificates.
         */
        private Map<String, String> hostMapping = new HashMap<String, String>();

        /**
         * @param mappedHost which host should be mapped
         * @param targetHost target host to which the {@code mappedHost} should be mapped
         */
        public void addHostMapping(String mappedHost, String targetHost)
        {
            hostMapping.put(mappedHost, targetHost);
        }

        @Override
        public final void verify(String host, String[] cns, String[] subjectAlts) throws javax.net.ssl.SSLException
        {
            String targetHost = hostMapping.get(host);
            if (targetHost != null) {
                boolean addHostAsSubjectAlt = false;
                for (String subjectAlt : subjectAlts) {
                    if (subjectAlt.equals(targetHost)) {
                        addHostAsSubjectAlt = true;
                    }
                    else if (subjectAlt.equals(host)) {
                        addHostAsSubjectAlt = false;
                        break;
                    }
                }
                if (addHostAsSubjectAlt) {
                    subjectAlts = Arrays.copyOf(subjectAlts, subjectAlts.length + 1);
                    subjectAlts[subjectAlts.length - 1] = host;
                }
            }
            verify(host, cns, subjectAlts, false);
        }
    }

    /**
     * SSL {@link javax.net.ssl.TrustManager} for trusting hosts.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private class TrustManager implements javax.net.ssl.X509TrustManager
    {
        /**
         * SSL and certificates constants.
         */
        private static final String JAVA_CA_CERT_FILE_NAME = "cacerts";
        private static final String CLASSIC_JAVA_CA_CERT_FILE_NAME = "jssecacerts";
        private static final int DEFAULT_HTTPS_PORT = 443;

        /**
         * Store of certificates.
         */
        private KeyStore certificateStore;

        /**
         * Default trust manager.
         */
        private javax.net.ssl.X509TrustManager defaultTrustManager;

        /**
         * Set of hosts whose certificates has already been added to the {@link #certificateStore}
         */
        private Set<String> alreadyAddedHosts = new HashSet<String>();

        /**
         * Constructor.
         *
         * @throws Exception
         */
        public TrustManager() throws Exception
        {
            File javaTrustStoreFile = findJavaTrustStoreFile();
            InputStream inputStream = new FileInputStream(javaTrustStoreFile);
            certificateStore = KeyStore.getInstance(KeyStore.getDefaultType());
            certificateStore.load(inputStream, null);
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
         * @param host from which all certificates should be added to the {@link #certificateStore}
         */
        public synchronized void addCertificates(String host)
        {
            if (alreadyAddedHosts.add(host)) {
                try {
                    javax.net.ssl.SSLContext tempConnectContext = javax.net.ssl.SSLContext.getInstance("TLS");
                    ExtractX509CertTrustManager x509CertTrustManager = new ExtractX509CertTrustManager();
                    tempConnectContext.init(null, new javax.net.ssl.TrustManager[]{x509CertTrustManager}, null);
                    javax.net.ssl.SSLSocketFactory socketFactory = tempConnectContext.getSocketFactory();
                    javax.net.ssl.SSLSocket socket =
                            (javax.net.ssl.SSLSocket) socketFactory.createSocket(host, DEFAULT_HTTPS_PORT);
                    socket.setSoTimeout(10000);
                    socket.startHandshake();
                    for (X509Certificate cert : x509CertTrustManager.getCurrentChain()) {
                        if (!certificateStore.isCertificateEntry(host)) {
                            certificateStore.setCertificateEntry(host, cert);
                        }
                    }
                    initDefaultTrustManager();
                    logger.info("Certificates for the host '{}' has been added to the certificate store.", host);
                }
                catch (Exception exception) {
                    logger.error(String.format("Certificates for the host '%s' failed to be added to the store.",
                            host), exception);
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
            javax.net.ssl.TrustManagerFactory trustManagerFactory = javax.net.ssl.TrustManagerFactory.getInstance(
                    javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(certificateStore);
            javax.net.ssl.TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            for (javax.net.ssl.TrustManager trustManager : trustManagers) {
                if (trustManager instanceof javax.net.ssl.X509TrustManager) {
                    defaultTrustManager = (javax.net.ssl.X509TrustManager) trustManager;
                    break;
                }
            }
        }

        /**
         * Trust Manager for the sole purpose of retrieving the X509 cert when a connection is made to a host we want
         * to start trusting.
         */
        private class ExtractX509CertTrustManager implements javax.net.ssl.X509TrustManager
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
        private File findJavaTrustStoreFile()
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


}
