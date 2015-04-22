package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.controller.api.UserSettings;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import org.apache.commons.cli.*;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.*;
import java.util.jar.Manifest;

/**
 * Shongo web client application.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ClientWeb
{
    private static Logger logger = LoggerFactory.getLogger(ClientWeb.class);

    /**
     * @return version of the {@link Connector}
     */
    public static String getVersion()
    {
        String filename = "version.properties";
        Properties properties = new Properties();
        InputStream inputStream = ClientWeb.class.getClassLoader().getResourceAsStream(filename);
        if (inputStream == null) {
            throw new RuntimeException("Properties file '" + filename + "' was not found in the classpath.");
        }
        try {
            try {
                properties.load(inputStream);
            }
            finally {
                inputStream.close();
            }
        }
        catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return properties.getProperty("version");
    }

    /**
     * Main method of web interface.
     *
     * @param arguments
     */
    public static void main(final String[] arguments) throws Exception
    {
        logger.info("ClientWeb {}", getVersion());

        Locale.setDefault(UserSettings.LOCALE_ENGLISH);

        // Create options
        Option optionHelp = new Option(null, "help", false, "Print this usage information");
        Option optionDaemon = OptionBuilder.withLongOpt("daemon")
                .withDescription("Web interface will be started as daemon not waiting to EOF")
                .create("d");
        Options options = new Options();
        options.addOption(optionHelp);
        options.addOption(optionDaemon);

        // Parse command line
        CommandLine commandLine = null;
        try {
            CommandLineParser parser = new PosixParser();
            commandLine = parser.parse(options, arguments);
        }
        catch (ParseException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        // Print help
        if (commandLine.hasOption(optionHelp.getLongOpt())) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(new Comparator<Option>()
            {
                public int compare(Option opt1, Option opt2)
                {
                    if (opt1.getOpt() == null && opt2.getOpt() != null) {
                        return -1;
                    }
                    if (opt1.getOpt() != null && opt2.getOpt() == null) {
                        return 1;
                    }
                    if (opt1.getOpt() == null && opt2.getOpt() == null) {
                        return opt1.getLongOpt().compareTo(opt2.getLongOpt());
                    }
                    return opt1.getOpt().compareTo(opt2.getOpt());
                }
            });
            formatter.printHelp("client-web", options);
            System.exit(0);
        }

        // Setup class-path for JAR file
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader instanceof URLClassLoader) {
            URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
            URL[] urls = urlClassLoader.getURLs();
            // Only when current class-path is single JAR file
            if (urls.length == 1 && urls[0].toExternalForm().endsWith(".jar")) {
                // Get directory from single JAR file
                File mainFile = new File(urls[0].toExternalForm());
                String path = mainFile.getParent();

                // Read class-path from manifest
                InputStream manifestStream = classLoader.getResourceAsStream("META-INF/MANIFEST.MF");
                String manifestClassPath;
                try {
                    Manifest manifest = new Manifest(manifestStream);
                    manifestClassPath = manifest.getMainAttributes().getValue("Class-Path");
                }
                finally {
                    manifestStream.close();
                }

                // Setup new class loader from the manifest class-path
                List<URL> newUrls = new LinkedList<URL>();
                for (String library : manifestClassPath.split(" ")) {
                    URL url = new URL(path + "/" + library);
                    newUrls.add(url);
                }
                urlClassLoader = new URLClassLoader(newUrls.toArray(new URL[newUrls.size()]));
                Thread.currentThread().setContextClassLoader(urlClassLoader);
            }
        }

        final ClientWebConfiguration clientWebConfiguration = ClientWebConfiguration.getInstance();
        final Server server = new Server();

        // Configure SSL
        ConfiguredSSLContext.getInstance().loadConfiguration(clientWebConfiguration);

        // Create web app
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setDefaultsDescriptor("WEB-INF/webdefault.xml");
        webAppContext.setDescriptor("WEB-INF/web.xml");
        webAppContext.setContextPath(clientWebConfiguration.getServerPath());
        webAppContext.setParentLoaderPriority(true);
        if (arguments.length > 0 && new File(arguments[0] + "/WEB-INF/web.xml").exists()) {
            String resourceBase = arguments[0];
            logger.info("Using '{}' as resource base.", resourceBase);
            webAppContext.setResourceBase(arguments[0]);
            clientWebConfiguration.setDefaultDesignFolderBasePath("file:" + resourceBase);
        }
        else {
            URL resourceBaseUrl = ClientWeb.class.getClassLoader().getResource("WEB-INF");
            if (resourceBaseUrl == null) {
                throw new RuntimeException("WEB-INF is not in classpath.");
            }
            String resourceBase = resourceBaseUrl.toExternalForm().replace("/WEB-INF", "/");
            webAppContext.setResourceBase(resourceBase);
        }

        // SSL key store
        final String sslKeyStore = clientWebConfiguration.getServerSslKeyStore();
        boolean forceHttps = sslKeyStore != null && clientWebConfiguration.isServerForceHttps();
        boolean forwarded = clientWebConfiguration.isServerForwarded();
        String forwardedHost = clientWebConfiguration.getServerForwardedHost();

        // Configure HTTP connector
        final SelectChannelConnector httpConnector;
        if (forceHttps) {
            httpConnector = new HttpsRedirectingSelectChannelConnector();
        }
        else {
            httpConnector = new SelectChannelConnector();
        }
        httpConnector.setPort(clientWebConfiguration.getServerPort());
        if (forwarded) {
            httpConnector.setForwarded(true);
            if (forwardedHost != null) {
                httpConnector.setHostHeader(forwardedHost);
            }
        }
        server.addConnector(httpConnector);

        // Configure HTTPS connector
        if (sslKeyStore != null) {
            if (forceHttps) {
                // Redirect HTTP to HTTPS
                httpConnector.setConfidentialPort(clientWebConfiguration.getServerSslPort());
                // Require confidential (forces the HTTP to HTTPS redirection)
                Constraint constraint = new Constraint();
                constraint.setDataConstraint(Constraint.DC_CONFIDENTIAL);
                ConstraintMapping constraintMapping = new ConstraintMapping();
                constraintMapping.setConstraint(constraint);
                constraintMapping.setPathSpec("/*");
                ConstraintSecurityHandler constraintSecurityHandler = new ConstraintSecurityHandler();
                constraintSecurityHandler.setConstraintMappings(new ConstraintMapping[]{constraintMapping});
                webAppContext.setSecurityHandler(constraintSecurityHandler);
            }

            final SslContextFactory sslContextFactory = new SslContextFactory(sslKeyStore);
            sslContextFactory.setKeyStorePassword(clientWebConfiguration.getServerSslKeyStorePassword());
            final SslSocketConnector httpsConnector = new SslSocketConnector(sslContextFactory);
            httpsConnector.setPort(clientWebConfiguration.getServerSslPort());
            if (forwarded) {
                httpsConnector.setForwarded(true);
                if (forwardedHost != null) {
                    httpsConnector.setHostHeader(forwardedHost);
                }
            }
            server.addConnector(httpsConnector);
        }

        // Configure shutdown hook
        Runnable shutdown = new Runnable()
        {
            private boolean handled = false;

            public void run()
            {
                try {
                    if (handled) {
                        return;
                    }
                    logger.info("Shutdown has been started...");
                    server.stop();
                    logger.info("Shutdown successfully completed.");
                }
                catch (Exception exception) {
                    logger.error("Shutdown failed", exception);
                }
                finally {
                    handled = true;
                }
            }
        };

        // Run client-web
        boolean waitEof = !commandLine.hasOption(optionDaemon.getOpt());
        try {
            server.setHandler(webAppContext);
            server.start();
            logger.info("ClientWeb successfully started.");

            // Request layout page to initialize
            logger.info("Initializing layout...");
            Connector serverConnector = server.getConnectors()[0];
            String serverHost = serverConnector.getHost();
            String serverUrl = String.format("http://%s:%d%s", (serverHost != null ? serverHost : "localhost"),
                    serverConnector.getLocalPort(), webAppContext.getContextPath());
            URLConnection serverConnection = new URL(serverUrl + "/layout").openConnection();
            serverConnection.getInputStream();
            logger.info("Layout successfully initialized.");

            // Configure shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(shutdown));

            if (waitEof) {
                // Shutdown when EOF reached
                while (System.in.read() != -1) {
                    continue;
                }
                shutdown.run();
            }
        }
        catch (Exception exception) {
            // Shutdown
            shutdown.run();
            throw exception;
        }
    }

    /**
     * Redirecting {@link SelectChannelConnector}.
     */
    public static class HttpsRedirectingSelectChannelConnector extends SelectChannelConnector
    {
        @Override
        public void customize(EndPoint endpoint, Request request) throws IOException
        {
            request.setScheme("https");
            super.customize(endpoint, request);
        }
    }
}
