package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.api.xmlrpc.Service;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.xmlrpc.WebServer;
import cz.cesnet.shongo.controller.api.xmlrpc.WebServerXmlLogger;
import cz.cesnet.shongo.controller.util.DatabaseHelper;
import cz.cesnet.shongo.jade.Container;
import cz.cesnet.shongo.jade.ContainerCommandSet;
import cz.cesnet.shongo.shell.CommandHandler;
import cz.cesnet.shongo.shell.Shell;
import cz.cesnet.shongo.util.ConsoleAppender;
import cz.cesnet.shongo.util.Logging;
import org.apache.commons.cli.*;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManagerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
     * Domain for which the controller is running.
     */
    private Domain domain = new Domain();

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
    WebServer rpcServer;

    /**
     * Jade container.
     */
    Container jadeContainer;

    /**
     * List of threads which are started for the controller.
     */
    List<Thread> threads = new ArrayList<Thread>();

    /**
     * Jade agent.
     */
    ControllerAgent jadeAgent = new ControllerAgent();

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
    }

    /**
     * @return {@link #domain}
     */
    public Domain getDomain()
    {
        return domain;
    }

    /**
     * Set domain information.
     *
     * @param name         sets the {@link Domain#name}
     * @param organization sets the {@link Domain#organization}
     */
    public void setDomain(String name, String organization)
    {
        domain.setName(name);
        domain.setOrganization(organization);
    }

    /**
     * @return {@link #authorization}
     */
    public Authorization getAuthorization()
    {
        return authorization;
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

        // Initialize domain
        if (domain == null) {
            throw new IllegalStateException(getClass().getName() + " doesn't have the domain set!");
        }
        if (domain.getName() == null) {
            domain.setName(configuration.getString(Configuration.DOMAIN_NAME));
        }
        if (domain.getOrganization() == null) {
            domain.setOrganization(configuration.getString(Configuration.DOMAIN_ORGANIZATION));
        }

        // Initialize authorization
        authorization = new Authorization(configuration);

        logger.info("Controller for domain '{}' is starting...", domain.getName());

        // Initialize components
        for (Component component : components) {
            if (component instanceof Component.DomainAware) {
                Component.DomainAware domainAware = (Component.DomainAware) component;
                domainAware.setDomain(domain);
            }
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

        rpcServer = new WebServer(rpcHost.isEmpty() ? null : rpcHost, getRpcPort());
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
        jadeContainer.addAgent("Controller", jadeAgent, null);
        if (jadeContainer.start() == false) {
            throw new IllegalStateException("Failed to start JADE container.");
        }
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
        shell.addCommand("log", "Toggle logging of [rpc|sql]", new CommandHandler()
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
        Options options = new Options();
        options.addOption(optionHost);
        options.addOption(optionRpcPort);
        options.addOption(optionJadePort);
        options.addOption(optionJadePlatform);
        options.addOption(optionHelp);

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

        logger.debug("Creating entity manager factory...");
        EntityManagerFactory entityManagerFactory = javax.persistence.Persistence.createEntityManagerFactory("controller");
        logger.debug("Entity manager factory created.");

        // Run controller
        Controller controller = new Controller("controller.cfg.xml");
        controller.setEntityManagerFactory(entityManagerFactory);

        // Add components
        Cache cache = new Cache();
        controller.addComponent(cache);
        controller.addComponent(new Preprocessor());
        Scheduler scheduler = new Scheduler();
        scheduler.setCache(cache);
        controller.addComponent(scheduler);
        controller.addComponent(new Executor());

        // Add XML-RPC services
        controller.addService(new CommonServiceImpl());
        ResourceServiceImpl resourceService = new ResourceServiceImpl();
        resourceService.setCache(cache);
        controller.addService(resourceService);
        controller.addService(new ResourceControlServiceImpl());
        controller.addService(new ReservationServiceImpl());
        controller.addService(new CompartmentServiceImpl());

        // Start, run and stop the controller
        controller.startAll();
        controller.run();
        controller.stop();
    }
}
