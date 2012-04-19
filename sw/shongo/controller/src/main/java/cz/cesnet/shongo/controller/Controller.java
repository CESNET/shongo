package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.common.shell.CommandHandler;
import cz.cesnet.shongo.common.shell.Shell;
import cz.cesnet.shongo.common.util.Logging;
import cz.cesnet.shongo.common.xmlrpc.Service;
import cz.cesnet.shongo.common.xmlrpc.WebServer;
import jade.core.Profile;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;

/**
 * Controller class
 *
 * @author Martin Srom
 */
public class Controller implements ApplicationContextAware
{
    private static Logger logger = LoggerFactory.getLogger(Controller.class);

    /**
     * Controller parameters
     */
    public static String rpcHost = null; // All interfaces
    public static int rpcPort = 8181;
    public static String agentHost = "127.0.0.1";
    public static int agentPort = 8282;

    /**
     * XML-RPC web server
     */
    WebServer rpcServer;

    /**
     * Jade container
     */
    jade.wrapper.ContainerController container;

    /**
     * Jade agent
     */
    jade.wrapper.AgentController agent;


    /**
     * Init controller
     */
    @PostConstruct
    public void start() throws Exception
    {
        logger.info("Starting Controller XML-RPC server on {}:{}...", (rpcHost == null ? "*" : rpcHost), rpcPort);

        try {
            rpcServer = new WebServer(rpcHost, rpcPort);

            // Fill services to web server
            Map<String, Service> services = applicationContext.getBeansOfType(Service.class);
            for (Service service : services.values()) {
                rpcServer.addHandler(service.getServiceName(), service);
            }

            // Start web server
            rpcServer.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Starting Controller Jade Agent on {}:{}...", agentHost, agentPort);

        jade.core.Runtime runtime = jade.core.Runtime.instance();
        runtime.setCloseVM(true);

        jade.core.Profile profile = new jade.core.ProfileImpl();
        profile.setParameter(Profile.PLATFORM_ID, "Shongo");
        profile.setParameter(Profile.CONTAINER_NAME, "Controller");
        profile.setParameter(Profile.LOCAL_HOST, agentHost);
        profile.setParameter(Profile.LOCAL_PORT, Integer.toString(agentPort));
        profile.setParameter(Profile.FILE_DIR, "data/jade/");
        new java.io.File("data/jade").mkdir();

        Logging.disableSystemOut();
        container = runtime.createMainContainer(profile);
        Logging.enableSystemOut();

        agent = container.createNewAgent("Controller", ControllerAgent.class.getCanonicalName(), null);
        agent.start();
    }

    /**
     * Run controller shell
     */
    public void run()
    {
        Shell shell = new Shell();
        shell.setPrompt("controller");
        shell.setExitCommand("exit", "Shutdown the controller");
        shell.addCommand("status", "Print status of the controller", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                status();
            }
        });
        shell.run();

        stop();
    }

    /**
     * Stop the controller
     */
    public void stop()
    {
        try {
            rpcServer.shutdown();
            agent.kill();
            container.kill();
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Print controller status
     */
    public void status()
    {
        System.out.println("TODO: Print status information about controller!");
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
                .withDescription("Set the port on which the controller XML-RPC server will run")
                .create("r");
        Option optionAgentPort = OptionBuilder.withLongOpt("agent-port")
                .withArgName("PORT")
                .hasArg()
                .withDescription("Set the port on which the controller Jade agent will run")
                .create("a");
        Options options = new Options();
        options.addOption(optionHost);
        options.addOption(optionRpcPort);
        options.addOption(optionAgentPort);
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
            agentHost = host;
        }
        if (commandLine.hasOption(optionRpcPort.getOpt())) {
            rpcPort = Integer.parseInt(commandLine.getOptionValue(optionRpcPort.getOpt()));
        }
        if (commandLine.hasOption(optionAgentPort.getOpt())) {
            agentPort = Integer.parseInt(commandLine.getOptionValue(optionAgentPort.getOpt()));
        }

        // Run application by spring application context
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring-context.xml");

        logger.info("Controller successfully started.");

        // Runn controller
        Controller controller = (Controller) applicationContext.getBean("controller");
        controller.run();
    }
}
