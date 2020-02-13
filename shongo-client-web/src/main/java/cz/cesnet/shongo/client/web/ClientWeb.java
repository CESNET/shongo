package cz.cesnet.shongo.client.web;

import com.google.common.base.Strings;
import cz.cesnet.shongo.controller.api.UserSettings;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import org.apache.commons.cli.*;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.annotations.ServletContainerInitializersStarter;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.*;

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

        // To control which parts of the container’s classpath should be processed
        webAppContext.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/[^/]*.jar$");

        // Establish Scratch directory for the servlet context (used by JSP compilation)
        webAppContext.setAttribute("javax.servlet.context.tempdir", getScratchDir());

        // Configure the application to support the compilation of JSP files.
        // We need a new class loader and some stuff so that Jetty can call the
        // onStartup() methods as required.
        webAppContext.setAttribute("org.eclipse.jetty.containerInitializers", jspInitializers());
        webAppContext.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
        webAppContext.addBean(new ServletContainerInitializersStarter(webAppContext), true);
        webAppContext.setClassLoader(new URLClassLoader(new URL[0], ClientWeb.class.getClassLoader()));


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
        HttpConfiguration httpConfig = new HttpConfiguration();
        ServerConnector httpConnector;
        if (forceHttps) {
            httpConfig.addCustomizer(new SecureRequestCustomizer());
            httpConfig.setSecureScheme("https");
            httpConfig.setSecurePort(clientWebConfiguration.getServerSslPort());
        }
        if (forwarded) {
            // Add support for X-Forwarded headers
            ForwardedRequestCustomizer forwardedCustomizer = new ForwardedRequestCustomizer();
            if (forwardedHost != null) {
                forwardedCustomizer.setForwardedHostHeader(forwardedHost);
            }
            httpConfig.addCustomizer(forwardedCustomizer);
        }

        httpConnector = new ServerConnector(server);
        httpConnector.addConnectionFactory(new HttpConnectionFactory(httpConfig));
        httpConnector.setPort(clientWebConfiguration.getServerPort());

        server.addConnector(httpConnector);

        // Configure HTTPS connector
        ServerConnector httpsConnector;
        if (sslKeyStore != null) {
            if (forceHttps) {
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

            final SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStorePath(sslKeyStore);
            sslContextFactory.setKeyStorePassword(clientWebConfiguration.getServerSslKeyStorePassword());

            HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
            httpsConfig.addCustomizer(new SecureRequestCustomizer());

            httpsConnector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.toString()),
                    new HttpConnectionFactory(httpsConfig));
            httpsConnector.setPort(clientWebConfiguration.getServerSslPort());

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
            Connector connector = server.getConnectors()[0];
            ServerConnector serverConnector = (ServerConnector) connector;
            String serverHost = serverConnector.getHost();
            String serverUrl = String.format("http://%s:%d%s", (!Strings.isNullOrEmpty(serverHost) ? serverHost : "localhost"),
                    serverConnector.getLocalPort(), webAppContext.getContextPath());
            URLConnection serverConnection = new URL(serverUrl + "layout").openConnection();
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

    private static File getScratchDir() throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File scratchDir = new File(tempDir.toString(), "jetty-jsp");

        if (!scratchDir.exists()) {
            if (!scratchDir.mkdirs()) {
                throw new IOException("Unable to create scratch directory: " + scratchDir);
            }
        }
        return scratchDir;
    }

    private static List<ContainerInitializer> jspInitializers() {
        JettyJasperInitializer sci = new JettyJasperInitializer();
        ContainerInitializer initializer = new ContainerInitializer(sci, null);
        List<ContainerInitializer> initializers = new ArrayList<ContainerInitializer>();
        initializers.add(initializer);
        return initializers;
    }

/*    *//**
     * Redirecting {@link SelectChannelConnector}.
     *//*
    public static class HttpsRedirectingSelectChannelConnector extends SelectChannelConnector
    {
        @Override
        public void customize(EndPoint endpoint, Request request) throws IOException
        {
            request.setScheme("https");
            super.customize(endpoint, request);
        }
    }*/
}
