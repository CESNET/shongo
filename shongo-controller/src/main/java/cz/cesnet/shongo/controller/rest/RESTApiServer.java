package cz.cesnet.shongo.controller.rest;

import com.google.common.base.Strings;
import cz.cesnet.shongo.controller.Controller;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.domains.BasicAuthFilter;
import cz.cesnet.shongo.controller.domains.SSLClientCertFilter;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import cz.cesnet.shongo.ssl.SSLCommunication;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.DispatcherType;
import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.EnumSet;

public class RESTApiServer
{
    private static final String SERVLET_PATH = "/";
    private static final String SERVLET_NAME = "rest-api";
    private static final String INTER_DOMAIN_API_PATH = "/domain/**";

    public static Server start(ControllerConfiguration configuration)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
    {
        Server restServer = new Server();
        ConfiguredSSLContext.getInstance().loadConfiguration(configuration);
        String resourceBase = RESTApiServer.getResourceBase();

        WebAppContext webAppContext = new WebAppContext();
        webAppContext.addServlet(new ServletHolder(SERVLET_NAME, DispatcherServlet.class), SERVLET_PATH);
        webAppContext.setResourceBase(resourceBase);
        webAppContext.setParentLoaderPriority(true);
        webAppContext.addFilter(
                new FilterHolder(new DelegatingFilterProxy("springSecurityFilterChain")),
                "/*", EnumSet.allOf(DispatcherType.class)
        );

        ServerConnector httpsConnector = createHTTPConnector(configuration, webAppContext, restServer);

        restServer.setConnectors(new Connector[]{httpsConnector});
        restServer.setHandler(webAppContext);

        try {
            restServer.start();
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        return restServer;
    }

    private static String getResourceBase()
    {
        URL resourceBaseUrl = Controller.class.getClassLoader().getResource("WEB-INF");
        if (resourceBaseUrl == null) {
            throw new RuntimeException("WEB-INF is not in classpath.");
        }
        return resourceBaseUrl.toExternalForm().replace("/WEB-INF", "/");
    }

    private static ServerConnector createHTTPConnector(
            ControllerConfiguration configuration,
            WebAppContext webAppContext,
            Server server)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
    {
        final HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecurePort(configuration.getRESTApiPort());
        ServerConnector serverConnector;

        final String sslKeyStore = configuration.getRESTApiSslKeyStore();
        if (sslKeyStore != null) {
            http_config.setSecureScheme(HttpScheme.HTTPS.asString());

            final HttpConfiguration https_config = new HttpConfiguration(http_config);
            https_config.addCustomizer(new SecureRequestCustomizer());

            final SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();

            sslContextFactory.setKeyStorePath(sslKeyStore);
            sslContextFactory.setKeyStorePassword(configuration.getRESTApiSslKeyStorePassword());
            String keystoreType = configuration.getRESTApiSslKeyStoreType();
            if (!Strings.isNullOrEmpty(keystoreType)) {
                sslContextFactory.setKeyStoreType(configuration.getRESTApiSslKeyStoreType());
            }

            if (configuration.isInterDomainConfigured()) {
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null);
                // Load certificates of foreign domain's CAs
                for (String certificatePath : configuration.getForeignDomainsCaCertFiles()) {
                    trustStore.setCertificateEntry(certificatePath.substring(0, certificatePath.lastIndexOf('.')),
                            SSLCommunication.readPEMCert(certificatePath));
                }
                sslContextFactory.setTrustStore(trustStore);

                if (configuration.requiresClientPKIAuth()) {
                    // Enable forced client auth
                    sslContextFactory.setNeedClientAuth(true);
                    // Enable SSL client filter by certificates
                    EnumSet<DispatcherType> filterTypes = EnumSet.of(DispatcherType.REQUEST);
                    webAppContext.addFilter(SSLClientCertFilter.class, INTER_DOMAIN_API_PATH, filterTypes);
                }
                else {
                    EnumSet<DispatcherType> filterTypes = EnumSet.of(DispatcherType.REQUEST);
                    webAppContext.addFilter(BasicAuthFilter.class, INTER_DOMAIN_API_PATH, filterTypes);
                }
            }
            serverConnector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory(https_config));
        }
        else {
            http_config.setSecureScheme(HttpScheme.HTTP.asString());
            serverConnector = new ServerConnector(server, new HttpConnectionFactory(http_config));
        }

        String host = configuration.getRESTApiHost();
        if (!Strings.isNullOrEmpty(host)) {
            serverConnector.setHost(host);
        }
        serverConnector.setPort(configuration.getRESTApiPort());

        return serverConnector;
    }
}
