package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.api.xmlrpc.Service;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.xmlrpc.RpcServer;
import cz.cesnet.shongo.controller.api.xmlrpc.WebServerXmlLogger;
import cz.cesnet.shongo.controller.notification.EmailNotificationExecutor;
import cz.cesnet.shongo.controller.notification.NotificationExecutor;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import cz.cesnet.shongo.controller.util.DatabaseHelper;
import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.jade.Container;
import cz.cesnet.shongo.jade.ContainerCommandSet;
import cz.cesnet.shongo.shell.CommandHandler;
import cz.cesnet.shongo.shell.Shell;
import cz.cesnet.shongo.util.ConsoleAppender;
import cz.cesnet.shongo.util.Logging;
import cz.cesnet.shongo.util.Timer;
import org.apache.commons.cli.*;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.IOException;
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
     * Configuration of the controller.
     */
    private Configuration configuration;

    /**
     * Entity manager factory.
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see Authorization
     */
    private Authorization authorization;

    /**
     * List of services of the domain controller.
     */
    private List<Service> services = new ArrayList<Service>();

    /**
     * List of components of the domain controller.
     */
    private List<Component> components = new ArrayList<Component>();

    /**
     * XML-RPC server.
     */
    private RpcServer rpcServer;

    /**
     * Jade container.
     */
    private Container jadeContainer;

    /**
     * List of threads which are started for the controller.
     */
    private List<Thread> threads = new ArrayList<Thread>();

    /**
     * Jade agent.
     */
    private ControllerAgent jadeAgent = new ControllerAgent();

    /**
     * {@link NotificationManager}.
     */
    private NotificationManager notificationManager = new NotificationManager();

    /**
     * Constructor.
     */
    public Controller()
    {
        setConfiguration(null);
    }

    /**
     * Constructor.
     *
     * @param configuration sets the {@link #configuration}
     */
    public Controller(org.apache.commons.configuration.Configuration configuration)
    {
        setConfiguration(configuration);
    }

    /**
     * Constructor.
     *
     * @param configurationFileName
     */
    public Controller(String configurationFileName)
    {
        try {
            XMLConfiguration xmlConfiguration = new XMLConfiguration();
            xmlConfiguration.setDelimiterParsingDisabled(true);
            xmlConfiguration.load(configurationFileName);
            setConfiguration(xmlConfiguration);
        }
        catch (Exception exception) {
            logger.warn(exception.getMessage());
            setConfiguration(null);
        }
    }

    /**
     * Destroy the controller.
     */
    public void destroy()
    {
        Domain.setLocalDomain(null);
    }

    /**
     * @return {@link #configuration}
     */
    public Configuration getConfiguration()
    {
        return configuration;
    }

    /**
     * @param configuration configuration to be set to the controller
     */
    public void setConfiguration(org.apache.commons.configuration.Configuration configuration)
    {
        this.configuration = new Configuration();
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
            xmlConfiguration.load(getClass().getClassLoader().getResource("default.cfg.xml"));
            this.configuration.addConfiguration(xmlConfiguration);
        }
        catch (Exception exception) {
            throw new RuntimeException("Failed to load default controller configuration!", exception);
        }

        // Initialize domain
        Domain localDomain = new Domain();
        localDomain.setName(this.configuration.getString(Configuration.DOMAIN_NAME));
        localDomain.setOrganization(this.configuration.getString(Configuration.DOMAIN_ORGANIZATION));
        Domain.setLocalDomain(localDomain);
    }

    /**
     * Set domain information.
     *
     * @param name         sets the {@link Domain#name}
     * @param organization sets the {@link Domain#organization}
     */
    public void setDomain(String name, String organization)
    {
        Domain localDomain = Domain.getLocalDomain();
        localDomain.setName(name);
        localDomain.setOrganization(organization);
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
        return configuration.getString(Configuration.RPC_HOST);
    }

    /**
     * @return XML-RPC server port
     */
    public int getRpcPort()
    {
        return configuration.getInt(Configuration.RPC_PORT);
    }

    /**
     * @return Jade container host
     */
    public String getJadeHost()
    {
        return configuration.getString(Configuration.JADE_HOST);
    }

    /**
     * @return Jade container host
     */
    public int getJadePort()
    {
        return configuration.getInt(Configuration.JADE_PORT);
    }

    /**
     * @return Jade platform id
     */
    public String getJadePlatformId()
    {
        return configuration.getString(Configuration.JADE_PLATFORM_ID);
    }

    /**
     * @param entityManagerFactory sets the {@link #entityManagerFactory}
     */
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * @param services sets the {@link #services}
     */
    public void setServices(List<Service> services)
    {
        this.services = services;
    }

    /**
     * @param service service to be added the {@link #services}
     */
    public synchronized void addService(Service service)
    {
        if (!services.contains(service)) {
            services.add(service);
            if (service instanceof Component) {
                addComponent((Component) service);
            }
        }
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
                addService((Service) component);
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
     * @return {@link #notificationManager}
     */
    public NotificationManager getNotificationManager()
    {
        return notificationManager;
    }

    /**
     * @param notificationExecutor to be added to the {@link #notificationManager}
     */
    public void addNotificationExecutor(NotificationExecutor notificationExecutor)
    {
        notificationManager.addNotificationExecutor(notificationExecutor);
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
    public void start() throws IllegalStateException
    {
        // Set single instance of domain controller.
        if (instance == null) {
            instance = this;
        }

        // Configure
        if (configuration.getBoolean(Configuration.LOG_RPC)) {
            WebServerXmlLogger.setEnabled(true);
        }

        // Initialize authorization
        authorization = Authorization.createInstance(configuration);

        logger.info("Controller for domain '{}' is starting...", Domain.getLocalDomain().getName());

        // Add common components
        addComponent(notificationManager);

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
                Component.NotificationManagerAware notificationManagerAware =
                        (Component.NotificationManagerAware) component;
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
    public void startAll() throws Exception
    {
        start();
        startRpc();
        startJade();
        startWorkerThread();
        startComponents();
    }

    /**
     * Start XML-RPC web server.
     *
     * @throws IOException
     */
    public void startRpc() throws IOException
    {
        String rpcHost = getRpcHost();
        logger.info("Starting Controller XML-RPC server on {}:{}...", (rpcHost.isEmpty() ? "*" : rpcHost),
                getRpcPort());

        rpcServer = new RpcServer(rpcHost.isEmpty() ? null : rpcHost, getRpcPort());
        for (Service service : services) {
            logger.debug("Adding XML-RPC service '" + service.getServiceName() + "'...");
            rpcServer.addHandler(service.getServiceName(), service);
        }
        rpcServer.start();
    }

    /**
     * Start JADE container.
     *
     * @throws IllegalStateException
     */
    public void startJade() throws IllegalStateException
    {
        logger.info("Starting Controller JADE container on {}:{} (platform {})...",
                new Object[]{getJadeHost(), getJadePort(), getJadePlatformId()});
        jadeContainer = Container.createMainContainer(getJadeHost(), getJadePort(), getJadePlatformId());
        if (jadeContainer.start() == false) {
            throw new IllegalStateException(
                    "Failed to start JADE container. Is not the port used by any other program?");
        }
        addJadeAgent("Controller", jadeAgent);
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
        boolean started;
        do {
            started = true;
            try {
                Thread.sleep(50);
            }
            catch (InterruptedException exception) {
            }
            for (String agentName : jadeContainer.getAgentNames()) {
                if (!jadeContainer.isAgentStarted(agentName)) {
                    started = false;
                }
            }
        }
        while (!started);
    }

    /**
     * Start worker thread which periodically runs preprocessor and scheduler
     */
    public void startWorkerThread()
    {
        WorkerThread workerThread = new WorkerThread(getComponent(Preprocessor.class), getComponent(Scheduler.class),
                entityManagerFactory);
        workerThread.setPeriod(configuration.getDuration(Configuration.WORKER_PERIOD));
        workerThread.setIntervalLength(configuration.getPeriod(Configuration.WORKER_INTERVAL));
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
    public void run()
    {
        Shell shell = new Shell();
        shell.setPrompt("controller");
        shell.setExitCommand("exit", "Shutdown the controller");
        shell.addCommands(ContainerCommandSet.createContainerCommandSet(jadeContainer));
        shell.addCommands(ContainerCommandSet.createContainerAgentCommandSet(jadeContainer, "Controller"));
        shell.addCommands(jadeAgent.createCommandSet());
        shell.addCommand("log", "Toggle logging of [rpc|sql|sql-param]", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                String[] args = commandLine.getArgs();
                if (args.length <= 1) {
                    return;
                }
                org.apache.log4j.Logger logger = null;
                Boolean enabled = null;
                if (args[1].equals("rpc")) {
                    enabled = !WebServerXmlLogger.isEnabled();
                    WebServerXmlLogger.setEnabled(enabled);
                    logger = org.apache.log4j.Logger.getLogger(
                            cz.cesnet.shongo.controller.api.xmlrpc.WebServerXmlLogger.class);
                }
                else if (args[1].equals("sql")) {
                    logger = org.apache.log4j.Logger.getLogger("org.hibernate.SQL");
                }
                else if (args[1].equals("sql-param")) {
                    logger = org.apache.log4j.Logger.getLogger("org.hibernate.type");
                }
                if (logger == null) {
                    return;
                }
                if (enabled == null) {
                    enabled = logger.getLevel() == null || logger.getLevel().isGreaterOrEqual(Level.INFO);
                }
                if (enabled) {
                    Controller.logger.info("Enabling '{}' logger.", args[1]);
                    logger.setLevel(Level.TRACE);
                }
                else {
                    Controller.logger.info("Disabling '{}' logger.", args[1]);
                    logger.setLevel(Level.INFO);
                }
            }
        });
        shell.addCommand("filter", "Filter logging by string (warning and errors are always not filtered)",
                new CommandHandler()
                {
                    @Override
                    public void perform(CommandLine commandLine)
                    {
                        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();
                        ConsoleAppender consoleAppender = (ConsoleAppender) logger.getAppender("CONSOLE");
                        String[] args = commandLine.getArgs();
                        String filter = null;
                        if (args.length > 1) {
                            filter = args[1].trim();
                            if (filter.equals("*")) {
                                filter = null;
                            }
                        }
                        consoleAppender.setFilter(null);
                        if (filter != null) {
                            Controller.logger.info("Enabling logger filter for '{}'.", filter);
                        }
                        else {
                            Controller.logger.info("Disabling logger filter.", filter);
                        }
                        consoleAppender.setFilter(filter);
                    }
                });
        shell.addCommand("database", "Show database browser", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                DatabaseHelper.runDatabaseManager(entityManagerFactory.createEntityManager());
            }
        });
        shell.run();
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
            rpcServer.stop();
        }

        // Destroy components
        for (Component component : components) {
            component.destroy();
        }
        // Destroy authorization
        authorization.destroy();

        logger.info("Controller exiting...");
    }

    /**
     * Single instance of controller that is created by spring context.
     */
    private static Controller instance;

    /**
     * @return {@link #instance}
     */
    public static Controller getInstance()
    {
        if (instance == null) {
            throw new IllegalStateException("Cannot get instance of a domain controller, "
                    + "because no controller has been created yet.");
        }
        return instance;
    }

    /**
     * Main controller method
     *
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
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
        Options options = new Options();
        options.addOption(optionHost);
        options.addOption(optionRpcPort);
        options.addOption(optionJadePort);
        options.addOption(optionJadePlatform);
        options.addOption(optionHelp);
        options.addOption(optionConfig);

        // Parse command line
        CommandLine commandLine = null;
        try {
            CommandLineParser parser = new PosixParser();
            commandLine = parser.parse(options, args);
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
            System.setProperty(Configuration.RPC_HOST, host);
            System.setProperty(Configuration.JADE_HOST, host);
        }
        if (commandLine.hasOption(optionRpcPort.getOpt())) {
            System.setProperty(Configuration.RPC_PORT, commandLine.getOptionValue(optionRpcPort.getOpt()));
        }
        if (commandLine.hasOption(optionJadePort.getOpt())) {
            System.setProperty(Configuration.JADE_PORT, commandLine.getOptionValue(optionJadePort.getOpt()));
        }
        if (commandLine.hasOption(optionJadePlatform.getOpt())) {
            System.setProperty(Configuration.JADE_PLATFORM_ID, commandLine.getOptionValue(optionJadePlatform.getOpt()));
        }

        // Get configuration file name
        String configurationFileName = "controller.cfg.xml";
        if (commandLine.hasOption(optionConfig.getOpt())) {
            configurationFileName = commandLine.getOptionValue(optionConfig.getOpt());
        }
        // Create controller
        Controller controller = new Controller(configurationFileName);

        logger.debug("Creating entity manager factory...");
        Timer timer = new Timer();
        // DatabaseMigration cannot be used because HyperSQL doesn't support transactional DDL
        // boolean development = Boolean.valueOf(System.getProperty("shongo.development"));
        // DatabaseMigration databaseMigration = new DatabaseMigration("controller",
        //         "cz.cesnet.shongo.controller.migration", (development ? "controller/src/main/java" : null));
        // EntityManagerFactory entityManagerFactory = databaseMigration.migrate();
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("hibernate.connection.url",
                String.format("jdbc:hsqldb:file:%s; shutdown=true; hsqldb.write_delay=false;",
                        controller.getConfiguration().getString(Configuration.DATABASE_FILENAME)));
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("controller", properties);
        logger.debug("Entity manager factory created in {} ms.", timer.stop());

        // Run controller
        controller.setEntityManagerFactory(entityManagerFactory);

        // Add components
        Cache cache = new Cache();
        controller.addComponent(cache);
        Preprocessor preprocessor = new Preprocessor();
        preprocessor.setCache(cache);
        controller.addComponent(preprocessor);
        Scheduler scheduler = new Scheduler();
        scheduler.setCache(cache);
        controller.addComponent(scheduler);
        controller.addComponent(new Executor());

        // Add mail notification executor
        controller.addNotificationExecutor(new EmailNotificationExecutor());

        // Add XML-RPC services
        controller.addService(new CommonServiceImpl());
        ResourceServiceImpl resourceService = new ResourceServiceImpl();
        resourceService.setCache(cache);
        controller.addService(resourceService);
        controller.addService(new ResourceControlServiceImpl());
        controller.addService(new ReservationServiceImpl());
        controller.addService(new ExecutorServiceImpl());

        // Start, run and stop the controller
        controller.startAll();
        logger.info("Controller successfully started.");
        controller.run();
        logger.info("Stopping controller...");
        controller.stop();
        controller.destroy();

        Container.killAllJadeThreads();
    }
}
