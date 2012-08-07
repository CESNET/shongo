package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.xmlrpc.Service;
import cz.cesnet.shongo.controller.api.xmlrpc.WebServer;
import cz.cesnet.shongo.jade.Container;
import cz.cesnet.shongo.jade.ContainerCommandSet;
import cz.cesnet.shongo.shell.Shell;
import cz.cesnet.shongo.util.Logging;
import org.apache.commons.cli.*;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.persistence.EntityManagerFactory;
import java.io.IOException;
import java.util.ArrayList;
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
     * Controller configuration parameters names.
     */
    public static final String DOMAIN_NAME = "domain.name";
    public static final String DOMAIN_ORGANIZATION = "domain.organization";
    public static final String RPC_HOST = "rpc.host";
    public static final String RPC_PORT = "rpc.port";
    public static final String JADE_HOST = "jade.host";
    public static final String JADE_PORT = "jade.port";
    public static final String JADE_PLATFORM_ID = "jade.platform-id";

    /**
     * Configuration of the controller.
     */
    private CompositeConfiguration configuration;

    /**
     * Domain for which the controller is running.
     */
    Domain domain = new Domain();

    /**
     * Entity manager factory.
     */
    EntityManagerFactory entityManagerFactory;

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
     * @see WorkerThread
     */
    WorkerThread workerThread;

    /**
     * Jade agent.
     */
    ControllerAgent jadeAgent;

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
    public Controller(Configuration configuration)
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
    public void setConfiguration(Configuration configuration)
    {
        this.configuration = new CompositeConfiguration();
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
     * @return XML-RPC server host
     */
    public String getRpcHost()
    {
        return configuration.getString(RPC_HOST);
    }

    /**
     * @return XML-RPC server port
     */
    public int getRpcPort()
    {
        return configuration.getInt(RPC_PORT);
    }

    /**
     * @return Jade container host
     */
    public String getJadeHost()
    {
        return configuration.getString(JADE_HOST);
    }

    /**
     * @return Jade container host
     */
    public int getJadePort()
    {
        return configuration.getInt(JADE_PORT);
    }

    /**
     * @return Jade platform id
     */
    public String getJadePlatformId()
    {
        return configuration.getString(JADE_PLATFORM_ID);
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
     * Start the domain controller (but do not start rpc web server or jade container).
     */
    public void start() throws IllegalStateException
    {
        // Set single instance of domain controller.
        if (instance == null) {
            instance = this;
        }

        // Initialize domain
        if (domain == null) {
            throw new IllegalStateException(getClass().getName() + " doesn't have the domain set!");
        }
        if (domain.getName() == null) {
            domain.setName(configuration.getString("domain.name"));
        }
        if (domain.getOrganization() == null) {
            domain.setOrganization(configuration.getString("domain.organization"));
        }

        logger.info("Controller for domain '{}' is starting...", domain.getName());

        // Initialize components
        for (Component component : components) {
            if (component instanceof Component.WithDomain) {
                Component.WithDomain componentWithDomain = (Component.WithDomain) component;
                componentWithDomain.setDomain(domain);
            }
            component.setEntityManagerFactory(entityManagerFactory);
            component.init();
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

        jadeAgent = new ControllerAgent();
        jadeContainer = Container.createMainContainer(getJadeHost(), getJadePort(), getJadePlatformId());
        jadeContainer.addAgent("Controller", jadeAgent);
        if (jadeContainer.start() == false) {
            throw new IllegalStateException("Failed to start JADE container.");
        }

        // Notify Component.ControllerAgentAware components
        for (Component component : components) {
            if (component instanceof Component.ControllerAgentAware) {
                Component.ControllerAgentAware controllerAgentAware = (Component.ControllerAgentAware) component;
                controllerAgentAware.setControllerAgent(jadeAgent);
            }
        }
    }

    /**
     * Start worker thread which periodically runs preprocessor and scheduler
     */
    public void startWorkerThread()
    {
        logger.info("Starting Controller worker...");
        workerThread = new WorkerThread(getComponent(Preprocessor.class), getComponent(Scheduler.class));
        workerThread.start();
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
        shell.run();
    }

    /**
     * Stop the controller and rpc web server or jade container if they are running
     */
    public void stop()
    {
        if (workerThread != null) {
            logger.info("Stopping Controller worker...");
            if (workerThread.isAlive()) {
                workerThread.interrupt();
                try {
                    workerThread.join();
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
    public static void main(String[] args)
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
            System.setProperty(RPC_HOST, host);
            System.setProperty(JADE_HOST, host);
        }
        if (commandLine.hasOption(optionRpcPort.getOpt())) {
            System.setProperty(RPC_PORT, commandLine.getOptionValue(optionRpcPort.getOpt()));
        }
        if (commandLine.hasOption(optionJadePort.getOpt())) {
            System.setProperty(JADE_PORT, commandLine.getOptionValue(optionJadePort.getOpt()));
        }
        if (commandLine.hasOption(optionJadePlatform.getOpt())) {
            System.setProperty(JADE_PLATFORM_ID, commandLine.getOptionValue(optionJadePlatform.getOpt()));
        }

        // Run application by spring application context
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring-context.xml");

        // Run controller
        Controller controller = (Controller) applicationContext.getBean("controller");
        controller.run();

        // Close spring application context
        applicationContext.close();
    }
}
