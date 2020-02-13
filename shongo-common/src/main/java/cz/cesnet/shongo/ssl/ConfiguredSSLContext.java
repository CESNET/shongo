package cz.cesnet.shongo.ssl;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AbstractVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
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

    /**
     * Configuration of SSL hostname verification.
     */
    public static final String SSL_MAPPED_HOSTNAME = "ssl.mapped-hostname";
    public static final String SSL_TRUSTED_HOSTNAME = "ssl.trusted-hostname";

    private TrustManager trustManager;

    private HostnameVerifier hostnameVerifier;

    private javax.net.ssl.SSLContext context;

    /**
     * Constructor.
     */
    public ConfiguredSSLContext()
    {
        try {
            // Create SSL context
            hostnameVerifier = new HostnameVerifier();
            trustManager = new TrustManager();
            context = javax.net.ssl.SSLContext.getInstance("TLSv1.2");
            context.init(null, new javax.net.ssl.TrustManager[]{trustManager}, new SecureRandom());

            // Set it as default context
            SSLContext.setDefault(context);
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
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
    public HostnameVerifier getHostnameVerifier()
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
     * @return new {@link HttpClient} configured with the {@link ConfiguredSSLContext}
     */
    public DefaultHttpClient createHttpClient()
    {
        return createHttpClient(30000);
    }

    /**
     * @return new {@link HttpClient} configured with the {@link ConfiguredSSLContext}
     */
    public DefaultHttpClient createHttpClient(int timeout)
    {
        ConfiguredSSLContext configuredSSLContext = getInstance();
        SchemeRegistry registry = new SchemeRegistry();
        org.apache.http.conn.ssl.SSLSocketFactory socketFactory = new org.apache.http.conn.ssl.SSLSocketFactory(
                configuredSSLContext.getContext(), configuredSSLContext.getHostnameVerifier());
        registry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        registry.register(new Scheme("https", 443, socketFactory));
        ClientConnectionManager connectionManager = new PoolingClientConnectionManager(registry);
        DefaultHttpClient httpClient = new DefaultHttpClient(connectionManager);
        HttpParams httpClientParams = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpClientParams, timeout);
        HttpConnectionParams.setSoTimeout(httpClientParams, timeout);
        return httpClient;
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


    /**
     * @param configuration to be loaded
     */
    public void loadConfiguration(HierarchicalConfiguration configuration)
    {
        ConfiguredSSLContext configuredSSLContext = ConfiguredSSLContext.getInstance();
        for (HierarchicalConfiguration mapping : configuration.configurationsAt(SSL_MAPPED_HOSTNAME)) {
            String source = mapping.getString("[@source]");
            String target = mapping.getString("[@target]");
            logger.info("Configuring SSL hostname verification to verify '{}' instead of '{}'.", target, source);
            configuredSSLContext.hostnameVerifier.addMappedHostName(source, target);
        }
        for (HierarchicalConfiguration mapping : configuration.configurationsAt(SSL_TRUSTED_HOSTNAME)) {
            String hostName = (String) mapping.getRoot().getValue();
            logger.info("Configuring SSL hostname verification to always successfully verify the '{}'.", hostName);
            configuredSSLContext.hostnameVerifier.addTrustedHostName(hostName);
        }
    }

    private class HostnameVerifier extends AbstractVerifier
    {
        /**
         * Set of mapped host names.
         */
        private Map<String, String> mappedHostNames = new HashMap<String, String>();

        /**
         * Set of trusted host names.
         */
        private Set<String> trustedHostNames = new HashSet<String>();

        /**
         * @param sourceHostName which host should be mapped
         * @param targetHostName target host to which the {@code mappedHostName} should be mapped
         */
        public void addMappedHostName(String sourceHostName, String targetHostName)
        {
            mappedHostNames.put(sourceHostName, targetHostName);
        }

        /**
         * @param trustedHostName to be added to the {@link #trustedHostNames}
         */
        public void addTrustedHostName(String trustedHostName)
        {
            trustedHostNames.add(trustedHostName);
        }

        @Override
        public final void verify(String hostName, String[] cns, String[] subjectAlts) throws javax.net.ssl.SSLException
        {
            boolean explicitlyTrustHostName = trustedHostNames.contains(hostName);
            String mappedHostName = mappedHostNames.get(hostName);
            if (explicitlyTrustHostName || mappedHostName != null) {
                Set<String> subjectAltsSet = new LinkedHashSet<String>();
                if (subjectAlts != null) {
                    subjectAltsSet.addAll(Arrays.asList(subjectAlts));
                    for (String subjectAlt : subjectAlts) {
                        if (subjectAlt.compareToIgnoreCase(mappedHostName) == 0) {
                            explicitlyTrustHostName = true;
                        }
                        else if (subjectAlt.compareToIgnoreCase(hostName) == 0) {
                            explicitlyTrustHostName = false;
                            break;
                        }
                    }
                }
                if (cns != null) {
                    for (String cn : cns) {
                        if (cn.compareToIgnoreCase(mappedHostName) == 0) {
                            explicitlyTrustHostName = true;
                        }
                    }
                }
                if (explicitlyTrustHostName) {
                    subjectAltsSet.add(hostName);
                }
                if (!subjectAltsSet.isEmpty()) {
                    subjectAlts = subjectAltsSet.toArray(new String[subjectAltsSet.size()]);
                }
            }
            verify(hostName, cns, subjectAlts, false);
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
            try {
                certificateStore = KeyStore.getInstance(KeyStore.getDefaultType());
                certificateStore.load(inputStream, null);
            }
            finally {
                inputStream.close();
            }
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
        public synchronized void addTrustedCertificate(String host)
        {
            if (alreadyAddedHosts.add(host)) {
                try {
                    URI hostUri = new URI(host);
                    host = hostUri.getHost();
                    if (host == null) {
                        host = hostUri.getPath();
                    }
                    javax.net.ssl.SSLContext tempConnectContext = javax.net.ssl.SSLContext.getInstance("TLS");
                    ExtractX509CertTrustManager x509CertTrustManager = new ExtractX509CertTrustManager();
                    tempConnectContext.init(null, new javax.net.ssl.TrustManager[]{x509CertTrustManager}, null);
                    javax.net.ssl.SSLSocketFactory socketFactory = tempConnectContext.getSocketFactory();
                    javax.net.ssl.SSLSocket socket =
                            (javax.net.ssl.SSLSocket) socketFactory.createSocket(host,
                                    (hostUri.getPort() != -1 ? hostUri.getPort() : DEFAULT_HTTPS_PORT));
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
                    logger.warn(String.format("Certificates for the host '%s' failed to be added to the store.",
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
