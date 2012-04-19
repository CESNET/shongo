package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.common.shell.CommandHandler;
import cz.cesnet.shongo.common.shell.Shell;
import cz.cesnet.shongo.common.util.Logging;
import jade.core.Profile;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

/**
 * Device connector main class.
 *
 * @author Martin Srom
 */
public class Connector
{
    private static Logger logger = LoggerFactory.getLogger(Connector.class);

    /**
     * Connector parameters
     */
    public static String agentHost = "127.0.0.1";
    public static int agentPort = 8383;
    public static String controllerHost = "127.0.0.1";
    public static int controllerPort = 8282;

    /**
     * Jade container
     */
    jade.wrapper.ContainerController container;

    /**
     * Jade agent
     */
    jade.wrapper.AgentController agent;

    /**
     * Init connector
     */
    public void start() throws Exception
    {
        logger.info("Starting Controller Jade Agent on {}:{}...", agentHost, agentPort);
        logger.info("Connecting to controller {}:{}...", controllerHost, controllerPort);

        jade.core.Runtime runtime = jade.core.Runtime.instance();
        runtime.setCloseVM(true);

        jade.core.Profile profile = new jade.core.ProfileImpl();
        profile.setParameter(Profile.LOCAL_HOST, agentHost);
        profile.setParameter(Profile.LOCAL_PORT, Integer.toString(agentPort));
        profile.setParameter(Profile.MAIN_HOST, controllerHost);
        profile.setParameter(Profile.MAIN_PORT, Integer.toString(controllerPort));
        profile.setParameter(Profile.FILE_DIR, "data/jade/");
        new java.io.File("data/jade").mkdir();

        Logging.disableSystemOut();
        container = runtime.createAgentContainer(profile);
        Logging.enableSystemOut();

        agent = container.createNewAgent("Connector", ConnectorAgent.class.getCanonicalName(), null);
        agent.start();
    }

    /**
     * Run connector shell
     */
    public void run()
    {
        Shell shell = new Shell();
        shell.setPrompt("connector");
        shell.setExitCommand("exit", "Shutdown the connector");
        shell.addCommand("status", "Print status of the connector", new CommandHandler()
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
     * De init connector
     */
    public void stop()
    {
        try {
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
        System.out.println("TODO: Print status information about connector!");
    }

    /**
     * Main method of device connector.
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
                .withDescription("Set the local interface address on which the connector Jade agent will run")
                .create("h");
        Option optionPort = OptionBuilder.withLongOpt("port")
                .withArgName("PORT")
                .hasArg()
                .withDescription("Set the port on which the connector Jade agent will run")
                .create("p");
        Option optionController = OptionBuilder.withLongOpt("controller")
                .withArgName("HOST:PORT")
                .hasArg()
                .withDescription("Set the url on which the controller is running")
                .create("c");
        Options options = new Options();
        options.addOption(optionHost);
        options.addOption(optionPort);
        options.addOption(optionController);
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
            formatter.printHelp("connector", options);
            System.exit(0);
        }

        // Process parameters
        if (commandLine.hasOption(optionHost.getOpt())) {
            agentHost = commandLine.getOptionValue(optionHost.getOpt());
        }
        if (commandLine.hasOption(optionPort.getOpt())) {
            agentPort = Integer.parseInt(commandLine.getOptionValue(optionPort.getOpt()));
        }
        if (commandLine.hasOption(optionController.getOpt())) {
            String url = commandLine.getOptionValue(optionController.getOpt());
            String[] urlParts = url.split(":");
            if (urlParts.length == 1) {
                controllerHost = urlParts[0];
            }
            else if (urlParts.length == 2) {
                controllerHost = urlParts[0];
                controllerPort = Integer.parseInt(urlParts[1]);
            }
            else {
                System.err.println("Failed to parse controller url. It should be in <HOST:URL> format.");
                System.exit(-1);
            }
        }

        Connector connector = new Connector();
        try {
            connector.start();
            connector.run();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        logger.info("Connector successfully started.");
    }
}
