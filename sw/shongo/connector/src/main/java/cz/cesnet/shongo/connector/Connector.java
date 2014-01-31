package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.api.util.Address;
import cz.cesnet.shongo.connector.api.ConnectorOptions;
import cz.cesnet.shongo.connector.jade.ConnectorContainerCommandSet;
import cz.cesnet.shongo.connector.jade.ManageLocalCommand;
import cz.cesnet.shongo.jade.Container;
import cz.cesnet.shongo.jade.ContainerCommandSet;
import cz.cesnet.shongo.shell.CommandHandler;
import cz.cesnet.shongo.shell.Shell;
import cz.cesnet.shongo.util.Logging;
import org.apache.commons.cli.*;
import org.apache.commons.configuration.*;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Represents a device connector.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Connector
{
    private static Logger logger = LoggerFactory.getLogger(Connector.class);

    /**
     * {@link Logger} for all JADE requested agent actions.
     */
    public static Logger requestedCommands =
            LoggerFactory.getLogger(Connector.class.getName() + ".RequestedCommand");

    /**
     * {@link Logger} for all JADE executed agent actions.
     */
    public static Logger executedCommands =
            LoggerFactory.getLogger(Connector.class.getName() + ".ExecutedCommand");

    /**
     * Default configuration filename.
     */
    public static final String DEFAULT_CONFIGURATION_FILENAME = "connector.cfg.xml";

    /**
     * Connector configuration.
     */
    private ConnectorConfiguration configuration = new ConnectorConfiguration();

    /**
     * Jade container.
     */
    private Container jadeContainer;

    /**
     * Jade agent names.
     * FIXME: agents are added, but not removed...
     */
    private List<String> jadeAgents = new ArrayList<String>();

    /**
     * Constructor.
     */
    public Connector()
    {
        configuration = new ConnectorConfiguration();
        // System properties has the highest priority
        configuration.addConfiguration(new SystemConfiguration());
    }

    /**
     * @return controller host
     */
    public String getControllerHost()
    {
        return configuration.getString(ConnectorConfiguration.CONTROLLER_HOST);
    }

    /**
     * @return controller host
     */
    public int getControllerPort()
    {
        return configuration.getInt(ConnectorConfiguration.CONTROLLER_PORT);
    }

    /**
     * @return period for checking connection the controller
     */
    public Duration getControllerConnectionCheckPeriod()
    {
        return configuration.getDuration(ConnectorConfiguration.CONTROLLER_CONNECTION_CHECK_PERIOD);
    }

    /**
     * @return Jade container host
     */
    public String getJadeHost()
    {
        return configuration.getString(ConnectorConfiguration.JADE_HOST);
    }

    /**
     * @return Jade container host
     */
    public int getJadePort()
    {
        return configuration.getInt(ConnectorConfiguration.JADE_PORT);
    }

    /**
     * Load default configuration for the connector
     */
    private void loadDefaultConfiguration()
    {
        try {
            XMLConfiguration xmlConfiguration = new XMLConfiguration();
            xmlConfiguration.setDelimiterParsingDisabled(true);
            xmlConfiguration.load(getClass().getClassLoader().getResource("default.cfg.xml"));
            configuration.addConfiguration(xmlConfiguration);
        }
        catch (Exception exception) {
            throw new RuntimeException("Failed to load default connector configuration!", exception);
        }
    }

    /**
     * Loads connector configuration from an XML file.
     *
     * @param configurationFilename name of file containing the connector configuration
     */
    private void loadConfiguration(String configurationFilename)
    {
        // Passed configuration has lower priority
        try {
            XMLConfiguration xmlConfiguration = new XMLConfiguration();
            xmlConfiguration.setDelimiterParsingDisabled(true);
            xmlConfiguration.load(configurationFilename);
            configuration.addConfiguration(xmlConfiguration);
        }
        catch (ConfigurationException e) {
            logger.warn(e.getMessage());
        }
        // Default configuration has the lowest priority
        loadDefaultConfiguration();
    }

    /**
     * Init connector.
     */
    public void start()
    {
        logger.info("Starting Connector JADE container on {}:{}...", getJadeHost(), getJadePort());
        logger.info("Connecting to the JADE main container {}:{}...", getControllerHost(), getControllerPort());

        jadeContainer = Container
                .createContainer(getControllerHost(), getControllerPort(), getJadeHost(), getJadePort());
        jadeContainer.start();

        // start configured agents
        for (HierarchicalConfiguration instCfg : configuration.configurationsAt("instances.instance")) {
            String agentName = instCfg.getString("name");
            addAgent(agentName, configuration);
        }
        configureAgents();
    }

    /**
     * Load agents configuration
     */
    public void configureAgents()
    {
        // Configure agents
        for (HierarchicalConfiguration instCfg : configuration.configurationsAt("instances.instance")) {
            String agentName = instCfg.getString("name");
            if (instCfg.getProperty("device.connector-class") != null) {
                ConnectorOptions connectorOptions = null;
                if (!instCfg.configurationsAt("device.options").isEmpty()) {
                    final HierarchicalConfiguration conf = instCfg.configurationAt("device.options");
                    connectorOptions = new ConfigurationConnectorOptions(conf);
                }

                // command the agent to manage a device
                ManageLocalCommand cmd = new ManageLocalCommand(
                        instCfg.getString("device.connector-class"),
                        instCfg.getString("device.host"),
                        instCfg.getInt("device.port", Address.DEFAULT_PORT),
                        instCfg.getString("device.auth.username"),
                        instCfg.getString("device.auth.password"),
                        connectorOptions
                );
                jadeContainer.performAgentLocalCommand(agentName, cmd);
            }
        }
    }

    /**
     * Run connector shell.
     */
    public void run()
    {
        final Shell shell = new Shell();
        shell.setPrompt("connector");
        shell.setExitCommand("exit", "Shutdown the connector");
        shell.addCommands(ContainerCommandSet.createContainerCommandSet(jadeContainer));

        shell.addCommand("add", "Add a new connector instance", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                String[] args = commandLine.getArgs();
                if (commandLine.getArgs().length < 2) {
                    Shell.printError("You must specify the new agent name.");
                    return;
                }
                addAgent(args[1], configuration);
            }
        });
        shell.addCommand("list", "List all connector agent instances", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                for (String agent : jadeAgents) {
                    Shell.printInfo("Connector [%s]", agent);
                }
            }
        });
        shell.addCommand("select", "Select current connector instance", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                String[] args = commandLine.getArgs();
                if (commandLine.getArgs().length < 2) {
                    shell.setPrompt("connector");
                    shell.removeCommands(
                            ConnectorContainerCommandSet.createContainerAgentCommandSet(jadeContainer, null));
                    return;
                }
                String agentName = args[1];
                for (String agent : jadeAgents) {
                    if (agent.equals(agentName)) {
                        shell.setPrompt(agentName + "@connector");
                        shell.addCommands(ConnectorContainerCommandSet.createContainerAgentCommandSet(jadeContainer, agentName));

                        return;
                    }
                }
                Shell.printError("Agent [%s] was not found!", agentName);
            }
        });

        // Thread that checks the connection to the main controller
        // and if it is down it tries to connect.
        final Thread connectThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                boolean startFailed = false;
                while (!Thread.interrupted()) {
                    try {
                        Thread.sleep(getControllerConnectionCheckPeriod().getMillis());
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        continue;
                    }
                    // We want to reconnect if container is not started or when the
                    // previous start failed
                    if (startFailed || jadeContainer.isStarted() == false) {
                        logger.warn("Reconnecting to the JADE main container {}:{}...", getControllerHost(),
                                getControllerPort());
                        startFailed = false;
                        if (jadeContainer.start()) {
                            configureAgents();
                        }
                        else {
                            startFailed = true;
                        }
                    }
                }
            }
        });
        connectThread.start();

        shell.run();

        connectThread.interrupt();

        stop();
    }

    /**
     * Adds an agent of a given name to the connector.
     *
     * @param name
     */
    private void addAgent(String name, ConnectorConfiguration configuration)
    {
        jadeContainer.addAgent(name, ConnectorAgent.class, new Object[]{configuration});
        jadeAgents.add(name);
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
     * @return version of the {@link Connector}
     */
    private static String getVersion()
    {
        String filename = "version.properties";
        Properties properties = new Properties();
        InputStream inputStream = Connector.class.getClassLoader().getResourceAsStream(filename);
        if (inputStream == null) {
            throw new RuntimeException("Properties file '" + filename + "' was not found in the classpath.");
        }
        try {
            properties.load(inputStream);
        }
        catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return properties.getProperty("version");
    }

    /**
     * Main method of device connector.
     *
     * @param args
     */
    public static void main(String[] args)
    {
        logger.info("Connector {}", getVersion());

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
        Option optionConfig = OptionBuilder.withLongOpt("config")
                .withArgName("FILENAME")
                .hasArg()
                .withDescription("Connector XML configuration file")
                .create("g");
        Options options = new Options();
        options.addOption(optionHost);
        options.addOption(optionPort);
        options.addOption(optionController);
        options.addOption(optionConfig);
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
            System.setProperty(ConnectorConfiguration.JADE_HOST, commandLine.getOptionValue(optionHost.getOpt()));
        }
        if (commandLine.hasOption(optionPort.getOpt())) {
            System.setProperty(ConnectorConfiguration.JADE_PORT, commandLine.getOptionValue(optionPort.getOpt()));
        }
        if (commandLine.hasOption(optionController.getOpt())) {
            String url = commandLine.getOptionValue(optionController.getOpt());
            String[] urlParts = url.split(":");
            if (urlParts.length == 1) {
                System.setProperty(ConnectorConfiguration.CONTROLLER_HOST, urlParts[0]);
            }
            else if (urlParts.length == 2) {
                System.setProperty(ConnectorConfiguration.CONTROLLER_HOST, urlParts[0]);
                System.setProperty(ConnectorConfiguration.CONTROLLER_PORT, urlParts[1]);
            }
            else {
                System.err.println("Failed to parse controller url. It should be in <HOST:URL> format.");
                System.exit(-1);
            }
        }

        final Connector connector = new Connector();

        // load configuration
        String configFilename = null;
        if (commandLine.hasOption(optionConfig.getOpt())) {
            configFilename = commandLine.getOptionValue(optionConfig.getOpt());
        }
        else {
            if (new File(DEFAULT_CONFIGURATION_FILENAME).exists()) {
                configFilename = DEFAULT_CONFIGURATION_FILENAME;
            }
        }
        if (configFilename != null) {
            logger.info("Connector loading configuration from {}", configFilename);
            connector.loadConfiguration(configFilename);
        }
        else {
            connector.loadDefaultConfiguration();
        }

        connector.start();

        logger.info("Connector successfully started.");

        connector.run();

        logger.info("Connector exiting...");

        Container.killAllJadeThreads();
    }
}
