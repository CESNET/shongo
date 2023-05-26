package cz.cesnet.shongo.controller.rest;

import com.google.common.base.Strings;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.domains.BasicAuthFilter;
import cz.cesnet.shongo.controller.domains.SSLClientCertFilter;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import cz.cesnet.shongo.ssl.SSLCommunication;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.DispatcherType;
import java.io.IOException;
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

        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.scan("cz.cesnet.shongo.controller");

        ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.addEventListener(new ContextLoaderListener(context));

        ServletHolder servletHolder = new ServletHolder(SERVLET_NAME, new DispatcherServlet(context));
        servletContextHandler.addServlet(servletHolder, SERVLET_PATH);

        FilterHolder springSecurityFilter = new FilterHolder(new DelegatingFilterProxy("springSecurityFilterChain"));
        servletContextHandler.addFilter(springSecurityFilter, "/*", EnumSet.allOf(DispatcherType.class));

        ServerConnector httpsConnector = createHTTPConnector(configuration, servletContextHandler, restServer);

        restServer.setConnectors(new Connector[]{httpsConnector});
        restServer.setHandler(servletContextHandler);

        try {
            restServer.start();
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        return restServer;
    }

    private static ServerConnector createHTTPConnector(
            ControllerConfiguration configuration,
            ServletContextHandler contextHandler,
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
                    contextHandler.addFilter(SSLClientCertFilter.class, INTER_DOMAIN_API_PATH, filterTypes);
                }
                else {
                    EnumSet<DispatcherType> filterTypes = EnumSet.of(DispatcherType.REQUEST);
                    contextHandler.addFilter(BasicAuthFilter.class, INTER_DOMAIN_API_PATH, filterTypes);
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
