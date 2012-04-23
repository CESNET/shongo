package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.common.jade.Container;
import cz.cesnet.shongo.common.jade.command.SendCommand;
import cz.cesnet.shongo.common.shell.CommandHandler;
import cz.cesnet.shongo.common.shell.Shell;
import cz.cesnet.shongo.common.util.Logging;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

/**
 * Represents a device connector.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Connector
{
    private static Logger logger = LoggerFactory.getLogger(Connector.class);

    /**
     * Connector parameters
     */
    public static String jadeHost = "127.0.0.1";
    public static int jadePort = 8383;
    public static String controllerHost = "127.0.0.1";
    public static int controllerPort = 8282;
    public static int reconnectTimeout = 10000;

    /**
     * Jade container
     */
    Container jadeContainer;

    /**
     * Init connector
     */
    public void start()
    {
        logger.info("Starting Connector JADE container on {}:{}...", jadeHost, jadePort);
        logger.info("Connecting to the JADE main container {}:{}...", controllerHost, controllerPort);

        jadeContainer = Container.createContainer(controllerHost, controllerPort, jadeHost, jadePort);
        jadeContainer.addAgent("Connector", ConnectorAgent.class);
        jadeContainer.start();
    }

    /**
     * Run connector shell
     */
    public void run()
    {
        final Shell shell = new Shell();
        shell.setPrompt("connector");
        shell.setExitCommand("exit", "Shutdown the connector");
        shell.addCommand("status", "Print status of the connector", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                jadeContainer.printStatus();
            }
        });
        shell.addCommand("send", "Send message to another agent", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                if (jadeContainer.isStarted() == false) {
                    Shell.printError("Cannot send message when the JADE container is not started.");
                    return;
                }
                String[] args = commandLine.getArgs();
                if (commandLine.getArgs().length < 3) {
                    Shell.printError("The send command requires two parameters: <AGENT> <MESSAGE>.");
                    return;
                }
                jadeContainer.performCommand("Connector", SendCommand.createSendMessage(args[1], args[2]));
            }
        });

        // Thread that checks the connection to the main controller
        // and if it is down it tries to connect.
        final Thread connectThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                boolean connected = false;
                while (true) {
                    try {
                        Thread.sleep(reconnectTimeout);
                    }
                    catch (InterruptedException e) {
                    }
                    if (connected == false || jadeContainer.isStarted() == false) {
                        logger.info("Reconnecting to the JADE main controller...");
                        connected = false;
                        if ( jadeContainer.start() ) {
                            connected = true;
                        }
                    }
                }
            }
        });
        connectThread.start();

        shell.run();

        connectThread.stop();

        stop();
    }

    /**
     * De init connector
     */
    public void stop()
    {
        logger.info("Stopping Connector JADE container...");
        jadeContainer.stop();
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
                .withDescription("Set the local interface address on which the connector Jade container will run")
                .create("h");
        Option optionPort = OptionBuilder.withLongOpt("port")
                .withArgName("PORT")
                .hasArg()
                .withDescription("Set the port on which the connector Jade container will run")
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
            jadeHost = commandLine.getOptionValue(optionHost.getOpt());
        }
        if (commandLine.hasOption(optionPort.getOpt())) {
            jadePort = Integer.parseInt(commandLine.getOptionValue(optionPort.getOpt()));
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

        final Connector connector = new Connector();
        connector.start();

        logger.info("Connector successfully started.");

        connector.run();

        logger.info("Connector exiting...");
    }
}
