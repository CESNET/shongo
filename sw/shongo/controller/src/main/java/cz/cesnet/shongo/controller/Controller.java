package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.common.jade.Container;
import cz.cesnet.shongo.common.jade.ContainerCommandSet;
import cz.cesnet.shongo.common.shell.Shell;
import cz.cesnet.shongo.common.util.Logging;
import cz.cesnet.shongo.common.xmlrpc.Service;
import cz.cesnet.shongo.common.xmlrpc.WebServer;
import cz.cesnet.shongo.controller.reservation.ReservationDatabase;
import cz.cesnet.shongo.controller.resource.ResourceDatabase;
import cz.cesnet.shongo.controller.scheduler.Scheduler;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Represents a domain controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Controller implements ApplicationContextAware
{
    private static Logger logger = LoggerFactory.getLogger(Controller.class);

    /**
     * Controller parameters.
     */
    public static String rpcHost = null; // All interfaces
    public static int rpcPort = 8181;
    public static String jadeHost = "127.0.0.1";
    public static int jadePort = 8282;
    public static String jadePlatformId = "Shongo";

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
     * Entity manager.
     */
    @Resource
    EntityManager entityManager;

    /**
     * Database of resources.
     */
    @Resource
    ResourceDatabase resourceDatabase;

    /**
     * Database of reservation requests.
     */
    @Resource
    ReservationDatabase reservationDatabase;

    /**
     * Scheduler of the domain controller.
     */
    @Resource
    Scheduler scheduler;

    /**
     * @return {@link #reservationDatabase}
     */
    public ResourceDatabase getResourceDatabase()
    {
        return resourceDatabase;
    }

    /**
     * @return {@link #reservationDatabase}
     */
    public ReservationDatabase getReservationDatabase()
    {
        return reservationDatabase;
    }

    /**
     * @return {@link #scheduler}
     */
    public Scheduler getScheduler()
    {
        return scheduler;
    }

    /**
     * Init controller.
     */
    @PostConstruct
    public void start() throws Exception
    {
        logger.info("Starting Controller XML-RPC server on {}:{}...", (rpcHost == null ? "*" : rpcHost), rpcPort);

        rpcServer = new WebServer(rpcHost, rpcPort);
        Map<String, Service> services = applicationContext.getBeansOfType(Service.class);
        for (Service service : services.values()) {
            rpcServer.addHandler(service.getServiceName(), service);
        }
        rpcServer.start();

        logger.info("Starting Controller JADE container on {}:{}...", jadeHost, jadePort);

        jadeAgent = new ControllerAgent();
        jadeContainer = Container.createMainContainer(jadeHost, jadePort, jadePlatformId);
        jadeContainer.addAgent("Controller", jadeAgent);
        if (jadeContainer.start() == false) {
            throw new Exception("Failed to start JADE container.");
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

        stop();
    }

    /**
     * Stop the controller
     */
    public void stop()
    {
        logger.info("Stopping Controller XML-RPC server...");
        rpcServer.stop();

        logger.info("Stopping Controller JADE container...");
        jadeContainer.stop();
    }

    /**
     * Application context
     */
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
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
            rpcHost = host;
            jadeHost = host;
        }
        if (commandLine.hasOption(optionRpcPort.getOpt())) {
            rpcPort = Integer.parseInt(commandLine.getOptionValue(optionRpcPort.getOpt()));
        }
        if (commandLine.hasOption(optionJadePort.getOpt())) {
            jadePort = Integer.parseInt(commandLine.getOptionValue(optionJadePort.getOpt()));
        }
        if (commandLine.hasOption(optionJadePlatform.getOpt())) {
            jadePlatformId = commandLine.getOptionValue(optionJadePlatform.getOpt());
        }

        // Run application by spring application context
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring-context.xml");

        logger.info("Controller successfully started.");

        // Run controller
        Controller controller = (Controller) applicationContext.getBean("controller");
        controller.run();

        // Close spring application context
        applicationContext.close();

        logger.info("Controller exiting...");
    }
}
