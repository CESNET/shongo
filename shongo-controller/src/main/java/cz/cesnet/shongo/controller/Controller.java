package cz.cesnet.shongo.controller;

import com.google.common.base.Strings;
import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.controller.api.UserSettings;
import cz.cesnet.shongo.controller.api.jade.ServiceImpl;
import cz.cesnet.shongo.controller.api.rpc.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.ServerAuthorization;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.calendar.CalendarManager;
import cz.cesnet.shongo.controller.calendar.connector.CalDAVConnector;
import cz.cesnet.shongo.controller.calendar.connector.CalendarConnector;
import cz.cesnet.shongo.controller.domains.BasicAuthFilter;
import cz.cesnet.shongo.controller.domains.InterDomainAgent;
import cz.cesnet.shongo.controller.domains.SSLClientCertFilter;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.notification.executor.EmailNotificationExecutor;
import cz.cesnet.shongo.controller.notification.executor.NotificationExecutor;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import cz.cesnet.shongo.controller.scheduler.Preprocessor;
import cz.cesnet.shongo.controller.scheduler.Scheduler;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.jade.Container;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import cz.cesnet.shongo.ssl.SSLCommunication;
import cz.cesnet.shongo.util.Logging;
import cz.cesnet.shongo.util.Timer;
import org.apache.commons.cli.*;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.DispatcherServlet;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.DispatcherType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;

/**
 * Represents a domain controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Controller
{
    private static Logger logger = LoggerFactory.getLogger(Controller.class);

    /**
     * {@link Logger} for all performed requests.
     */
    public static Logger loggerApi = LoggerFactory.getLogger(Controller.class.getName() + ".Api");

    /**
     * {@link Logger} for all performed requests.
     */
    public static Logger loggerAcl = LoggerFactory.getLogger(Controller.class.getName() + ".Acl");

    /**
     * {@link Logger} for all JADE requested agent actions.
     */
    public static Logger loggerRequestedCommands =
            LoggerFactory.getLogger(Controller.class.getName() + ".RequestedCommand");

    /**
     * {@link Logger} for all JADE executed agent actions.
     */
    public static Logger loggerExecutedCommands =
            LoggerFactory.getLogger(Controller.class.getName() + ".ExecutedCommand");

    /**
     * Configuration of the controller.
     */
    protected ControllerConfiguration configuration;

    /**
     * {@link org.joda.time.DateTimeZone} old default timezone.
     */
    private DateTimeZone oldDefaultTimeZone;

    /**
     * Entity manager factory.
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see cz.cesnet.shongo.controller.authorization.Authorization
     */
    private Authorization authorization;

    /**
     * List of components of the domain controller.
     */
    private List<Component> components = new ArrayList<Component>();

    /**
     * XML-RPC server.
     */
    private org.eclipse.jetty.server.Server rpcServer;

    /**
     * List of services of the domain controller.
     */
    private List<Service> rpcServices = new ArrayList<Service>();

    /**
     * Jade container.
     */
    protected Container jadeContainer;

    /**
     * InterDomain REST server
     */
    protected Server restServer;

    /**
     * If {@code restServer} is running and {@link InterDomainAgent} is initialized.
     */
    protected boolean interDomainInitialized = false;

    /**
     * List of threads which are started for the controller.
     */
    private List<Thread> threads = new ArrayList<Thread>();

    /**
     * Jade agent.
     */
    protected ControllerAgent jadeAgent;

    /**
     * @see cz.cesnet.shongo.controller.Reporter
     */
    private Reporter reporter;

    /**
     * @see EmailSender
     */
    private EmailSender emailSender;

    /**
     * @see NotificationManager
     */
    private NotificationManager notificationManager = new NotificationManager();

    private CalDAVConnector calendarConnector;

    private CalendarManager calendarManager = new CalendarManager();

    /**
     * Constructor.
     *
     * @param configuration sets the {@link #configuration}
     */
    protected Controller(org.apache.commons.configuration.AbstractConfiguration configuration)
    {
        setConfiguration(configuration);
    }

    /**
     * Destroy the controller.
     */
    public void destroy()
    {
        // Destroy reporter
        reporter.destroy();

        // Reset single instance of controller
        instance = null;

        // Local domain
        LocalDomain.setLocalDomain(null);

        // Default time zone
        if (oldDefaultTimeZone != null) {
            logger.info("Configuring default timezone back to {}.", oldDefaultTimeZone.getID());
            DateTimeZone.setDefault(oldDefaultTimeZone);
        }
    }

    /**
     * @return {@link #configuration}
     */
    public ControllerConfiguration getConfiguration()
    {
        return configuration;
    }

    /**
     * @param configuration configuration to be set to the controller
     */
    private void setConfiguration(org.apache.commons.configuration.AbstractConfiguration configuration)
    {
        this.configuration = new ControllerConfiguration();
        // System properties has the highest priority
        this.configuration.addConfiguration(new SystemConfiguration());
        // Passed configuration has lower priority
        if (configuration != null) {
            this.configuration.addConfiguration(configuration);
        }
        // Default configuration has the lowest priority
        try {
            XMLConfiguration xmlConfiguration = new XMLConfiguration();
            xmlConfiguration.setDelimiterParsingDisabled(true);
            xmlConfiguration.load(getClass().getClassLoader().getResource("controller-default.cfg.xml"));
            this.configuration.addConfiguration(xmlConfiguration);
        }
        catch (Exception exception) {
            throw new RuntimeException("Failed to load default controller configuration!", exception);
        }

        // Initialize default locale
        Locale defaultLocale = UserSettings.LOCALE_ENGLISH;
        logger.info("Configuring default locale to {}.", defaultLocale);
        Locale.setDefault(defaultLocale);

        // Initialize default timezone
        String timeZoneId = this.configuration.getString(ControllerConfiguration.TIMEZONE);
        if (timeZoneId != null && !timeZoneId.isEmpty()) {
            oldDefaultTimeZone = DateTimeZone.getDefault();
            DateTimeZone dateTimeZone = DateTimeZone.forID(timeZoneId);
            logger.info("Configuring default timezone to {}.", dateTimeZone.getID());
            DateTimeZone.setDefault(dateTimeZone);
            TimeZone.setDefault(dateTimeZone.toTimeZone());
        }

        // Initialize domain
        LocalDomain localDomain = new LocalDomain();
        localDomain.setName(this.configuration.getString(ControllerConfiguration.DOMAIN_NAME));
        localDomain.setShortName(this.configuration.getString(ControllerConfiguration.DOMAIN_SHORT_NAME));
        localDomain.setOrganization(this.configuration.getString(ControllerConfiguration.DOMAIN_ORGANIZATION));
        LocalDomain.setLocalDomain(localDomain);

        //Create CalDAVConnector
        this.calendarConnector = new CalDAVConnector(this.configuration);

        // Create email sender
        this.emailSender = new EmailSender(this.configuration);

        // Create jade agent
        this.jadeAgent = new ControllerAgent(this.configuration);
    }

    /**
     * @return {@link #entityManagerFactory}
     */
    public EntityManagerFactory getEntityManagerFactory()
    {
        return entityManagerFactory;
    }

    /**
     * Set domain information.
     *
     * @param name         sets the {@link LocalDomain#name}
     * @param organization sets the {@link LocalDomain#organization}
     */
    public void setDomain(String name, String organization)
    {
        LocalDomain localDomain = LocalDomain.getLocalDomain();
        localDomain.setName(name);
        localDomain.setOrganization(organization);
    }

    /**
     * @param authorization sets the {@link #authorization}
     */
    public void setAuthorization(Authorization authorization)
    {
        this.authorization = authorization;
    }

    /**
     * @return {@link #authorization}
     */
    public Authorization getAuthorization()
    {
        return authorization;
    }

    /**
     * @return {@link #jadeAgent}
     */
    public ControllerAgent getAgent()
    {
        return jadeAgent;
    }

    /**
     * @return XML-RPC server host
     */
    public String getRpcHost()
    {
        return configuration.getRpcHost(false);
    }

    /**
     * @return XML-RPC server port
     */
    public int getRpcPort()
    {
        return configuration.getRpcPort();
    }

    /**
     * @return Jade container host
     */
    public String getJadeHost()
    {
        return configuration.getString(ControllerConfiguration.JADE_HOST);
    }

    /**
     * @return Jade container host
     */
    public int getJadePort()
    {
        return configuration.getInt(ControllerConfiguration.JADE_PORT);
    }

    /**
     * @return {@link #jadeContainer}
     */
    public Container getJadeContainer()
    {
        return jadeContainer;
    }

    /**
     * @return Jade platform id
     */
    public String getJadePlatformId()
    {
        return configuration.getString(ControllerConfiguration.JADE_PLATFORM_ID);
    }

    public static boolean isInterDomainAgentRunning()
    {
        try {
            return getInstance().isInterDomainInitialized() && getInstance().restServer.isRunning();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param entityManagerFactory sets the {@link #entityManagerFactory}
     */
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * @param rpcServices sets the {@link #rpcServices}
     */
    public void setRpcServices(List<Service> rpcServices)
    {
        this.rpcServices = rpcServices;
    }

    /**
     * @param rpcService service to be added the {@link #rpcServices}
     */
    public synchronized void addRpcService(Service rpcService)
    {
        if (!rpcServices.contains(rpcService)) {
            rpcServices.add(rpcService);
            if (rpcService instanceof Component) {
                addComponent((Component) rpcService);
            }
        }
    }

    /**
     * @param jadeService sets the {@link ControllerAgent#setService(cz.cesnet.shongo.controller.api.jade.Service)}
     */
    public void setJadeService(cz.cesnet.shongo.controller.api.jade.Service jadeService)
    {
        jadeAgent.setService(jadeService);
    }

    /**
     * @param components sets the {@link #components}
     */
    public void setComponents(List<Component> components)
    {
        this.components = components;
    }

    /**
     * @param component component to be added to the {@link #components}
     */
    public synchronized void addComponent(Component component)
    {
        if (!components.contains(component)) {
            components.add(component);
            if (component instanceof Service) {
                addRpcService((Service) component);
            }
        }
    }

    /**
     * @param componentType
     * @return component of given type
     */
    public <T> T getComponent(Class<T> componentType)
    {
        for (Component component : components) {
            if (componentType.isInstance(component)) {
                return componentType.cast(component);
            }
        }
        return null;
    }

    /**
     * @param throwInternalErrorsForTesting sets the {@link #reporter#setThrowInternalErrorsForTesting}
     */
    public void setThrowInternalErrorsForTesting(boolean throwInternalErrorsForTesting)
    {
        reporter.setThrowInternalErrorsForTesting(throwInternalErrorsForTesting);
    }

    /**
     * @return {@link #emailSender}
     */
    public EmailSender getEmailSender()
    {
        return emailSender;
    }

    public CalendarConnector getCalendarConnector()
    {
        return calendarConnector;
    }

    /**
     * @return {@link #notificationManager}
     */
    public NotificationManager getNotificationManager()
    {
        return notificationManager;
    }

    public CalendarManager getCalendarManager()
    {
        return calendarManager;
    }

    /**
     * @param notificationExecutor to be added to the {@link #notificationManager}
     */
    public void addNotificationExecutor(NotificationExecutor notificationExecutor)
    {
        notificationManager.addNotificationExecutor(notificationExecutor);
    }

    public void addCalendarConnector (CalendarConnector calendarConnector)
    {
        calendarManager.addCalendarConnector(calendarConnector);
    }

    /**
     * @param thread to be started and added to the {@link #threads}
     */
    private void addThread(Thread thread)
    {
        logger.debug("Starting thread [{}]...", thread.getName());
        threads.add(thread);
        thread.start();
    }

    /**
     * Start the domain controller (but do not start rpc web server or jade container).
     */
    public void start()
    {
        // Check authorization
        if (authorization == null) {
            throw new IllegalStateException("Authorization is not set.");
        }

        logger.info("Controller for domain '{}' is starting...", LocalDomain.getLocalDomain().getName());

        // Add common components
        addComponent(notificationManager);
        addComponent(calendarManager);

        // Initialize components
        for (Component component : components) {
            if (component instanceof Component.EntityManagerFactoryAware) {
                Component.EntityManagerFactoryAware entityManagerFactoryAware = (Component.EntityManagerFactoryAware) component;
                entityManagerFactoryAware.setEntityManagerFactory(entityManagerFactory);
            }
            if (component instanceof Component.ControllerAgentAware) {
                Component.ControllerAgentAware controllerAgentAware = (Component.ControllerAgentAware) component;
                controllerAgentAware.setControllerAgent(jadeAgent);
            }
            if (component instanceof Component.AuthorizationAware) {
                Component.AuthorizationAware authorizationAware = (Component.AuthorizationAware) component;
                authorizationAware.setAuthorization(authorization);
            }
            if (component instanceof Component.NotificationManagerAware) {
                Component.NotificationManagerAware notificationManagerAware = (Component.NotificationManagerAware) component;
                notificationManagerAware.setNotificationManager(notificationManager);
            }
            component.init(configuration);
        }
    }

    /**
     * Start the domain controller and also the rpc web server and jade container.
     *
     * @throws Exception
     */
    public void startAll() throws Exception {
        start();
        startRpc();
        startJade();
        startInterDomainRESTApi();
        startWorkerThread();
        startComponents();
    }

    /**
     * Start XML-RPC web server.
     *
     * @throws IOException
     */
    public void startRpc() throws Exception
    {
        logger.info("Starting Controller XML-RPC server on {}:{}...", getRpcHost(), getRpcPort());

        RpcServlet rpcServlet = new RpcServlet();
        for (Service rpcService : rpcServices) {
            logger.debug("Adding XML-RPC service '" + rpcService.getServiceName() + "'...");
            rpcServlet.addHandler(rpcService.getServiceName(), rpcService);
        }

        rpcServer = new org.eclipse.jetty.server.Server();
        final String sslKeyStore = configuration.getRpcSslKeyStore();
        final String rpcHost = configuration.getRpcHost(true);
        final HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecurePort(getRpcPort());

        if (sslKeyStore != null) {
            // Configure HTTPS connector
            http_config.setSecureScheme(HttpScheme.HTTPS.asString());
            final HttpConfiguration https_config = new HttpConfiguration(http_config);
            https_config.addCustomizer(new SecureRequestCustomizer());
            final SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStorePath(sslKeyStore);
            sslContextFactory.setKeyStorePassword(configuration.getRpcSslKeyStorePassword());

            final ServerConnector httpsConnector = new ServerConnector(rpcServer,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory(https_config));
            if (rpcHost != null) {
                httpsConnector.setHost(rpcHost);
            }
            httpsConnector.setPort(getRpcPort());
            httpsConnector.setIdleTimeout(50000);
            rpcServer.addConnector(httpsConnector);
        }
        else {
            // Configure HTTP connector
            http_config.setSecureScheme("http");

            final ServerConnector httpConnector = new ServerConnector(rpcServer,
                    new HttpConnectionFactory(http_config));
//            final org.eclipse.jetty.server.nio.SelectChannelConnector httpConnector =
//                    new org.eclipse.jetty.server.nio.SelectChannelConnector();
            if (rpcHost != null) {
                httpConnector.setHost(rpcHost);
            }
            httpConnector.setPort(getRpcPort());
            rpcServer.addConnector(httpConnector);
        }

        org.eclipse.jetty.servlet.ServletHandler servletHandler = new org.eclipse.jetty.servlet.ServletHandler();
        ServletHolder servletHolder = new ServletHolder("XmlRpcServlet", rpcServlet);
        servletHandler.addServlet(servletHolder);
        ServletMapping servletMapping = new ServletMapping();
        servletMapping.setPathSpec("/");
        servletMapping.setServletName(servletHolder.getName());
        servletHandler.addServletMapping(servletMapping);
        rpcServer.setHandler(servletHandler);
        rpcServer.start();
    }

    /**
     * Start JADE container.
     */
    public Container startJade()
    {
        logger.info("Starting Controller JADE container on {}:{} (platform {})...",
                new Object[]{getJadeHost(), getJadePort(), getJadePlatformId()});

        // Start jade container
        jadeContainer = Container.createMainContainer(getJadeHost(), getJadePort(), getJadePlatformId());
        if (!jadeContainer.start()) {
            throw new RuntimeException(
                    "Failed to start JADE container. Is not the port used by any other program?");
        }

        // Add jade agent
        addJadeAgent(configuration.getString(ControllerConfiguration.JADE_AGENT_NAME), jadeAgent);

        return jadeContainer;
    }

    public Server startInterDomainRESTApi() throws NoSuchAlgorithmException, CertificateException, InvalidAlgorithmParameterException, IOException, KeyStoreException {
        if (configuration.isInterDomainConfigured()) {
            logger.info("Starting Inter Domain REST server on {}:{}...",
                    configuration.getInterDomainHost(), configuration.getInterDomainPort());

            restServer = new Server();
            // Configure SSL
            ConfiguredSSLContext.getInstance().loadConfiguration(configuration);

            // Create web app
            WebAppContext webAppContext = new WebAppContext();
            String servletPath = "/*";
            webAppContext.addServlet(new ServletHolder("interDomain", DispatcherServlet.class), servletPath);
            webAppContext.setParentLoaderPriority(true);

            URL resourceBaseUrl = Controller.class.getClassLoader().getResource("WEB-INF");
            if (resourceBaseUrl == null) {
                throw new RuntimeException("WEB-INF is not in classpath.");
            }
            String resourceBase = resourceBaseUrl.toExternalForm().replace("/WEB-INF", "/");
            webAppContext.setResourceBase(resourceBase);

            final HttpConfiguration http_config = new HttpConfiguration();

            // Configure HTTPS connector
            http_config.setSecureScheme(HttpScheme.HTTPS.asString());
            http_config.setSecurePort(configuration.getInterDomainPort());
            final HttpConfiguration https_config = new HttpConfiguration(http_config);
            https_config.addCustomizer(new SecureRequestCustomizer());


            final SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null);
            // Load certificates of foreign domain's CAs
            for (String certificatePath : configuration.getForeignDomainsCaCertFiles()) {
                trustStore.setCertificateEntry(certificatePath.substring(0, certificatePath.lastIndexOf('.')),
                        SSLCommunication.readPEMCert(certificatePath));
            }
            sslContextFactory.setKeyStorePath(configuration.getInterDomainSslKeyStore());
            sslContextFactory.setKeyStoreType(configuration.getInterDomainSslKeyStoreType());
            sslContextFactory.setKeyStorePassword(configuration.getInterDomainSslKeyStorePassword());
            sslContextFactory.setTrustStore(trustStore);
            if (configuration.requiresClientPKIAuth()) {
                // Enable forced client auth
                sslContextFactory.setNeedClientAuth(true);
                // Enable SSL client filter by certificates
                EnumSet<DispatcherType> filterTypes = EnumSet.of(DispatcherType.REQUEST);
                webAppContext.addFilter(SSLClientCertFilter.class, servletPath, filterTypes);
            }
            else {
                EnumSet<DispatcherType> filterTypes = EnumSet.of(DispatcherType.REQUEST);
                webAppContext.addFilter(BasicAuthFilter.class, servletPath, filterTypes);
            }

            final ServerConnector httpsConnector = new ServerConnector(restServer,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory(https_config));
            String host = configuration.getInterDomainHost();
            if (!Strings.isNullOrEmpty(host)) {
                httpsConnector.setHost(host);
            }
            httpsConnector.setPort(configuration.getInterDomainPort());
            httpsConnector.setIdleTimeout(configuration.getInterDomainCommandTimeout());



            restServer.setConnectors(new Connector[]{httpsConnector});

            restServer.setHandler(webAppContext);
            try {
                restServer.start();
                logger.info("Inter Domain REST server successfully started.");
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }

            this.interDomainInitialized = true;
            return restServer;
        }
        return null;
    }

    /**
     * Add new JADE agent to the container.
     *
     * @param agentName name of the agent
     * @param agent     instance of the agent
     */
    public <T extends Agent> T addJadeAgent(String agentName, T agent)
    {
        jadeContainer.addAgent(agentName, agent, null);
        return agent;
    }

    /**
     * Wait for all JADE agents to start.
     */
    public void waitForJadeAgentsToStart()
    {
        jadeContainer.waitForJadeAgentsToStart();
    }

    /**
     * Start worker thread which periodically runs preprocessor and scheduler
     */
    public void startWorkerThread()
    {
        WorkerThread workerThread = new WorkerThread(getComponent(Preprocessor.class), getComponent(Scheduler.class),
                notificationManager, calendarManager, entityManagerFactory);
        workerThread.setPeriod(configuration.getDuration(ControllerConfiguration.WORKER_PERIOD));
        workerThread.setLookahead(configuration.getPeriod(ControllerConfiguration.WORKER_LOOKAHEAD));
        addThread(workerThread);
    }

    /**
     * Start components.
     */
    public void startComponents()
    {
        for (Component component : components) {
            if (component instanceof Component.WithThread) {
                Component.WithThread withThread = (Component.WithThread) component;
                Thread thread = withThread.getThread();
                addThread(thread);
            }
        }
    }

    /**
     * Run controller shell
     */
    public void runShell()
    {
        ControllerShell controllerShell = new ControllerShell(this);
        controllerShell.run();
    }

    /**
     * Stop the controller and rpc web server or jade container if they are running
     */
    public void stop()
    {
        List<Thread> reverseThreads = new ArrayList<Thread>();
        reverseThreads.addAll(threads);
        Collections.reverse(reverseThreads);
        for (Thread thread : reverseThreads) {
            logger.debug("Stopping thread [{}]...", thread.getName());
            if (thread.isAlive()) {
                thread.interrupt();
                try {
                    thread.join();
                }
                catch (Exception e) {
                }
            }
        }

        if (jadeContainer != null) {
            logger.info("Stopping Controller JADE container...");
            jadeContainer.stop();
        }

        if (rpcServer != null) {
            logger.info("Stopping Controller XML-RPC server...");
            try {
                rpcServer.stop();
            }
            catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        if (restServer != null) {
            logger.info("Stopping Controller Inter Domain REST server...");
            try {
                restServer.stop();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        // Destroy components
        for (Component component : components) {
            component.destroy();
        }
        // Destroy authorization
        if (authorization != null) {
            authorization.destroy();
        }

        logger.info("Controller exiting...");
    }

    /**
     * Single instance of controller that is created by spring context.
     */
    private static Controller instance;

    /**
     * Constructor.
     */
    public static Controller create()
    {
        return create(new Controller(null));
    }

    /**
     * Constructor.
     *
     * @param configurationFileName
     */
    public static Controller create(String configurationFileName)
    {
        Controller controller;
        try {
            XMLConfiguration xmlConfiguration = new XMLConfiguration();
            xmlConfiguration.setDelimiterParsingDisabled(true);
            xmlConfiguration.load(configurationFileName);
            controller = new Controller(xmlConfiguration);
        }
        catch (Exception exception) {
            logger.warn(exception.getMessage());
            controller = new Controller(null);
        }
        return create(controller);
    }

    /**
     * Constructor.
     *
     * @param controller
     */
    public static synchronized Controller create(Controller controller)
    {
        if (instance != null) {
            throw new IllegalStateException("Another instance of controller already exists.");
        }
        controller.reporter = Reporter.create(controller);
        instance = controller;
        return instance;
    }

    /**
     * @return {@link #instance}
     */
    public static synchronized Controller getInstance()
    {
        if (instance == null) {
            throw new IllegalStateException("Cannot get instance of a domain controller, "
                    + "because no controller has been created yet.");
        }
        return instance;
    }

    /**
     * @return true whether {@link #instance} is not null,
     *         false otherwise
     */
    public static boolean hasInstance()
    {
        return instance != null;
    }

    public static boolean isInterDomainInitialized()
    {
        try {
            return getInstance().interDomainInitialized;
        }
        catch (IllegalStateException ex) {
            // Controller is not initialized (in tests)
            return false;
        }
    }

    /**
     * @return version of the {@link Controller}
     */
    private static String getVersion()
    {
        String filename = "version.properties";
        Properties properties = new Properties();
        InputStream inputStream = Controller.class.getClassLoader().getResourceAsStream(filename);
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
     * Initialize database.
     *
     * @param entityManagerFactory
     */
    public static void initializeDatabase(EntityManagerFactory entityManagerFactory)
    {
        String initQuery = NativeQuery.getNativeQuery(entityManagerFactory, NativeQuery.INIT);

        logger.debug("Initializing database...");

        Timer timer = new Timer();
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        NativeQuery.executeNativeUpdate(entityManager, initQuery);
        entityManager.close();

        logger.debug("Database initialized in {} ms.", timer.stop());
    }

    /**
     * Main controller method
     *
     * @param arguments
     */
    public static void main(String[] arguments) throws Exception
    {
        logger.info("Controller {}", getVersion());

        Logging.installBridge();

        // Create options
        Option optionHelp = new Option(null, "help", false, "Print this usage information");
        Option optionHost = OptionBuilder.withLongOpt("host")
                .withArgName("HOST")
                .hasArg()
                .withDescription("Set the local interface address on which the controller will run")
                .create("h");
        Option optionRpcPort = OptionBuilder.withLongOpt("rpc-port")
                .withArgName("PORT")
                .hasArg()
                .withDescription("Set the port on which the XML-RPC server will run")
                .create("r");
        Option optionJadePort = OptionBuilder.withLongOpt("jade-port")
                .withArgName("PORT")
                .hasArg()
                .withDescription("Set the port on which the JADE main controller will run")
                .create("a");
        Option optionJadePlatform = OptionBuilder.withLongOpt("jade-platform")
                .withArgName("PLATFORM")
                .hasArg()
                .withDescription("Set the platform-id for the JADE main controller")
                .create("p");
        Option optionConfig = OptionBuilder.withLongOpt("config")
                .withArgName("FILENAME")
                .hasArg()
                .withDescription("Controller XML configuration file")
                .create("g");
        Option optionDaemon = OptionBuilder.withLongOpt("daemon")
                .withDescription("Controller will be started as daemon without the interactive shell")
                .create("d");
        Options options = new Options();
        options.addOption(optionHost);
        options.addOption(optionRpcPort);
        options.addOption(optionJadePort);
        options.addOption(optionJadePlatform);
        options.addOption(optionHelp);
        options.addOption(optionConfig);
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
            formatter.printHelp("controller", options);
            System.exit(0);
        }

        // Process parameters
        if (commandLine.hasOption(optionHost.getOpt())) {
            String host = commandLine.getOptionValue(optionHost.getOpt());
            System.setProperty(ControllerConfiguration.RPC_HOST, host);
            System.setProperty(ControllerConfiguration.JADE_HOST, host);
        }
        if (commandLine.hasOption(optionRpcPort.getOpt())) {
            System.setProperty(ControllerConfiguration.RPC_PORT, commandLine.getOptionValue(optionRpcPort.getOpt()));
        }
        if (commandLine.hasOption(optionJadePort.getOpt())) {
            System.setProperty(ControllerConfiguration.JADE_PORT, commandLine.getOptionValue(optionJadePort.getOpt()));
        }
        if (commandLine.hasOption(optionJadePlatform.getOpt())) {
            System.setProperty(ControllerConfiguration.JADE_PLATFORM_ID, commandLine.getOptionValue(optionJadePlatform.getOpt()));
        }

        // Get configuration file name
        String configurationFileName = "shongo-controller.cfg.xml";
        if (commandLine.hasOption(optionConfig.getOpt())) {
            configurationFileName = commandLine.getOptionValue(optionConfig.getOpt());
        }
        // Create controller
        final Controller controller = Controller.create(configurationFileName);
        ControllerConfiguration configuration = controller.getConfiguration();
        NotificationManager notificationManager = controller.getNotificationManager();
        CalendarManager calendarManager = controller.getCalendarManager();

        // Configure SSL
        ConfiguredSSLContext.getInstance().loadConfiguration(configuration);

        logger.debug("Creating entity manager factory...");
        Timer timer = new Timer();
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("hibernate.connection.driver_class",
                controller.getConfiguration().getString(ControllerConfiguration.DATABASE_DRIVER));
        properties.put("hibernate.connection.url",
                controller.getConfiguration().getString(ControllerConfiguration.DATABASE_URL));
        properties.put("hibernate.connection.username",
                controller.getConfiguration().getString(ControllerConfiguration.DATABASE_USERNAME));
        properties.put("hibernate.connection.password",
                controller.getConfiguration().getString(ControllerConfiguration.DATABASE_PASSWORD));
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("controller", properties);
        logger.debug("Entity manager factory created in {} ms.", timer.stop());

        Controller.initializeDatabase(entityManagerFactory);

        // Setup controller
        ServerAuthorization authorization = ServerAuthorization.createInstance(configuration, entityManagerFactory);
        controller.setAuthorization(authorization);
        controller.setEntityManagerFactory(entityManagerFactory);

        // Add components
        Cache cache = new Cache();
        controller.addComponent(cache);
        Preprocessor preprocessor = new Preprocessor();
        preprocessor.setCache(cache);
        controller.addComponent(preprocessor);
        Scheduler scheduler = new Scheduler(cache, notificationManager, calendarManager);
        controller.addComponent(scheduler);
        Executor executor = new Executor(notificationManager);
        controller.addComponent(executor);

        // Add mail notification executor
        controller.addNotificationExecutor(new EmailNotificationExecutor(controller.getEmailSender(), configuration));

        controller.addCalendarConnector(controller.getCalendarConnector());

        // Initialize Inter Domain agent
        if (configuration.isInterDomainConfigured()) {
            InterDomainAgent.create(entityManagerFactory, configuration, authorization, controller.getEmailSender(), cache);
        }

        // Add XML-RPC services
        RecordingsCache recordingsCache = new RecordingsCache();
        controller.addRpcService(new CommonServiceImpl());
        controller.addRpcService(new AuthorizationServiceImpl());
        controller.addRpcService(new ResourceServiceImpl(cache));
        controller.addRpcService(new ResourceControlServiceImpl(recordingsCache));
        controller.addRpcService(new ReservationServiceImpl(cache));
        controller.addRpcService(new ExecutableServiceImpl(executor, recordingsCache));

        // Add JADE service
        controller.setJadeService(new ServiceImpl(entityManagerFactory, notificationManager, executor, authorization));

        // Prepare shutdown runnable
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
                    logger.info("Stopping controller...");
                    controller.stop();
                    controller.destroy();
                    Container.killAllJadeThreads();
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

        // Run controller
        boolean shell = !commandLine.hasOption(optionDaemon.getOpt());
        try {
            // Start
            controller.startAll();
            authorization.initRootAccessToken();
            logger.info("Controller successfully started.");

            // Configure shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(shutdown));

            if (shell) {
                // Run shell
                controller.runShell();
                // Shutdown
                shutdown.run();
            }
        }
        catch (Exception exception) {
            // Shutdown
            shutdown.run();
            throw exception;
        }
    }
}
