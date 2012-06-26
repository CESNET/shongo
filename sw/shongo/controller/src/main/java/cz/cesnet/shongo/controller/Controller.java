package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.common.jade.Container;
import cz.cesnet.shongo.common.jade.ContainerCommandSet;
import cz.cesnet.shongo.common.shell.Shell;
import cz.cesnet.shongo.common.util.Logging;
import cz.cesnet.shongo.common.xmlrpc.Service;
import cz.cesnet.shongo.common.xmlrpc.WebServer;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.annotation.PostConstruct;
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
     * Default controller parameters values.
     */
    public static final String  DEFAULT_RPC_HOST  = ""; // All interfaces
    public static final Integer DEFAULT_RPC_PORT  = 8181;
    public static final String  DEFAULT_JADE_HOST = "127.0.0.1";
    public static final Integer DEFAULT_JADE_PORT = 8282;
    public static final String  DEFAULT_JADE_PLATFORM_ID = "Shongo";

    /**
     * Host to run XML-RPC web server.
     */
    private String rpcHost;

    /**
     * Port to run XML-RPC web server.
     */
    private Integer rpcPort;

    /**
     * Host to run JADE container.
     */
    private String jadeHost;

    /**
     * Port to run JADE container.
     */
    private Integer jadePort;

    /**
     * JADE platform identifier.
     */
    private String jadePlatformId;

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
     * Jade agent.
     */
    ControllerAgent jadeAgent;

    /**
     * Constructor.
     */
    public Controller()
    {
    }

    /**
     * @return {@link #rpcHost}
     */
    public String getRpcHost()
    {
        return rpcHost;
    }

    /**
     * @param rpcHost sets the {@link #rpcHost}
     */
    public void setRpcHost(String rpcHost)
    {
        this.rpcHost = rpcHost;
    }

    /**
     * @return {@link #rpcPort}
     */
    public Integer getRpcPort()
    {
        return rpcPort;
    }

    /**
     * @param rpcPort sets the {@link #rpcPort}
     */
    public void setRpcPort(int rpcPort)
    {
        this.rpcPort = rpcPort;
    }

    /**
     * @param jadeHost sets the {@link #jadeHost}
     */
    public void setJadeHost(String jadeHost)
    {
        this.jadeHost = jadeHost;
    }

    /**
     * @param jadePort sets the {@link #jadePort}
     */
    public void setJadePort(int jadePort)
    {
        this.jadePort = jadePort;
    }

    /**
     * @param jadePlatformId sets the {@link #jadePlatformId}
     */
    public void setJadePlatformId(String jadePlatformId)
    {
        this.jadePlatformId = jadePlatformId;
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
            if ( service instanceof Component) {
                addComponent((Component)service);
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
            if ( component instanceof Service) {
                addService((Service)component);
            }
        }
    }

    /**
     * Start the domain controller.
     */
    public void start() throws IllegalStateException
    {
        // Set single instance of domain controller.
        if (instance != null) {
            throw new IllegalStateException("A domain controller has already been created, cannot create second one!");
        }
        instance = this;

        logger.info("Controller is starting...");

        // Inititialize components
        for (Component component : components) {
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
    }

    /**
     * Start XML-RPC web server.
     *
     * @throws IOException
     */
    public void startRpc() throws IOException
    {
        if ( rpcHost == null ) {
            rpcHost = DEFAULT_RPC_HOST;
        }
        if ( rpcPort == null ) {
            rpcPort = DEFAULT_RPC_PORT;
        }

        logger.info("Starting Controller XML-RPC server on {}:{}...", (rpcHost.isEmpty() ? "*" : rpcHost), rpcPort);

        rpcServer = new WebServer(rpcHost.isEmpty() ? null : rpcHost, rpcPort);
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
        if ( jadeHost == null ) {
            jadeHost = DEFAULT_JADE_HOST;
        }
        if ( jadePort == null ) {
            jadePort = DEFAULT_JADE_PORT;
        }
        if ( jadePlatformId == null ) {
            jadePlatformId = DEFAULT_JADE_PLATFORM_ID;
        }

        logger.info("Starting Controller JADE container on {}:{}...", jadeHost, jadePort);

        jadeAgent = new ControllerAgent();
        jadeContainer = Container.createMainContainer(jadeHost, jadePort, jadePlatformId);
        jadeContainer.addAgent("Controller", jadeAgent);
        if (jadeContainer.start() == false) {
            throw new IllegalStateException("Failed to start JADE container.");
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
        shell.run();
    }

    /**
     * Stop the controller
     */
    public void stop()
    {
        if ( jadeContainer != null ) {
            logger.info("Stopping Controller JADE container...");
            jadeContainer.stop();
        }

        if ( rpcServer != null ) {
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

        System.setProperty("rpc-host", DEFAULT_RPC_HOST);
        System.setProperty("rpc-port", DEFAULT_RPC_PORT.toString());
        System.setProperty("jade-host", DEFAULT_JADE_HOST);
        System.setProperty("jade-port", DEFAULT_JADE_PORT.toString());
        System.setProperty("jade-platform-id", DEFAULT_JADE_PLATFORM_ID);

        // Process parameters
        if (commandLine.hasOption(optionHost.getOpt())) {
            String host = commandLine.getOptionValue(optionHost.getOpt());
            System.setProperty("rpc-host", host);
            System.setProperty("jade-host", host);
        }
        if (commandLine.hasOption(optionRpcPort.getOpt())) {
            System.setProperty("rpc-port", commandLine.getOptionValue(optionRpcPort.getOpt()));
        }
        if (commandLine.hasOption(optionJadePort.getOpt())) {
            System.setProperty("jade-port", commandLine.getOptionValue(optionJadePort.getOpt()));
        }
        if (commandLine.hasOption(optionJadePlatform.getOpt())) {
            System.setProperty("jade-platform-id", commandLine.getOptionValue(optionJadePlatform.getOpt()));
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
