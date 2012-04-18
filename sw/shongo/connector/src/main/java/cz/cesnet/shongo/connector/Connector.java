package cz.cesnet.shongo.connector;

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
     * Init connector
     */
    public void init()
    {
        logger.info("Starting Controller Jade Agent on {}:{}...", agentHost, agentPort);
        logger.info("Connecting to controller {}:{}...", controllerHost, controllerPort);
    }

    /**
     * Main method of device connector.
     *
     * @param args
     */
    public static void main(String[] args)
    {
        cz.cesnet.shongo.common.util.Logging.installBridge();

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
        connector.init();

        logger.info("Connector successfully started.");
    }
}
