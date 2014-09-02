package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.connector.api.ConnectorConfiguration;
import cz.cesnet.shongo.connector.common.ConnectorConfigurationImpl;
import cz.cesnet.shongo.connector.jade.ConnectorAgent;
import cz.cesnet.shongo.connector.jade.ConnectorContainerCommandSet;
import cz.cesnet.shongo.jade.Container;
import cz.cesnet.shongo.jade.ContainerCommandSet;
import cz.cesnet.shongo.shell.CommandHandler;
import cz.cesnet.shongo.shell.Shell;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
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
public class ConnectorContainer
{
    private static Logger logger = LoggerFactory.getLogger(ConnectorContainer.class);

    /**
     * {@link Logger} for all JADE requested agent actions.
     */
    public static Logger requestedCommands =
            LoggerFactory.getLogger(ConnectorContainer.class.getName() + ".RequestedCommand");

    /**
     * {@link Logger} for all JADE executed agent actions.
     */
    public static Logger executedCommands =
            LoggerFactory.getLogger(ConnectorContainer.class.getName() + ".ExecutedCommand");

    /**
     * Default configuration file names.
     */
    public static final String[] DEFAULT_CONFIGURATION_FILE_NAMES = new String[]{
            "shongo-connector.cfg.xml",
            "shongo-connector.auth.xml"
    };

    /**
     * Connector configuration.
     */
    private ConnectorContainerConfiguration configuration;

    /**
     * List of {@link cz.cesnet.shongo.connector.api.ConnectorConfiguration}.
     */
    private List<ConnectorConfiguration> connectorConfigurations = new LinkedList<ConnectorConfiguration>();

    /**
     * Jade container.
     */
    private Container jadeContainer;

    /**
     * Constructor.
     *
     * @param configuration sets the {@link #configuration}
     */
    public ConnectorContainer(ConnectorContainerConfiguration configuration)
    {
        this.configuration = configuration;

        // Add connector configurations
        for (CombinedConfiguration combinedConfiguration : configuration.getConnectorConfigurations()) {
            if (combinedConfiguration.getString("class") == null) {
                continue;
            }
            addConnectorConfiguration(new ConnectorConfigurationImpl(combinedConfiguration));
        }
    }

    /**
     * Constructor.
     */
    public ConnectorContainer()
    {
        this(new ConnectorContainerConfiguration());
    }

    /**
     * Constructor.
     *
     * @param configuration
     */
    public ConnectorContainer(AbstractConfiguration configuration)
    {
        this(new ConnectorContainerConfiguration(configuration));
    }

    /**
     * Constructor.
     *
     * @param configurationFileNames
     */
    public ConnectorContainer(List<String> configurationFileNames)
    {
        this(new ConnectorContainerConfiguration(
                ConnectorContainerConfiguration.loadConfigurations(configurationFileNames)));
    }

    /**
     * @return {@link #configuration}
     */
    public ConnectorContainerConfiguration getConfiguration()
    {
        return configuration;
    }

    /**
     * @return controller host
     */
    public String getControllerHost()
    {
        return configuration.getString(ConnectorContainerConfiguration.CONTROLLER_HOST);
    }

    /**
     * @return controller host
     */
    public int getControllerPort()
    {
        return configuration.getInt(ConnectorContainerConfiguration.CONTROLLER_PORT);
    }

    /**
     * @return period for checking connection the controller
     */
    public Duration getControllerConnectionCheckPeriod()
    {
        return configuration.getDuration(ConnectorContainerConfiguration.CONTROLLER_CONNECTION_CHECK_PERIOD);
    }

    /**
     * @return Jade container host
     */
    public String getJadeHost()
    {
        return configuration.getString(ConnectorContainerConfiguration.JADE_HOST);
    }

    /**
     * @return Jade container host
     */
    public int getJadePort()
    {
        return configuration.getInt(ConnectorContainerConfiguration.JADE_PORT);
    }

    /**
     * @param connectorConfiguration to be added to the {@link #connectorConfigurations}
     */
    public void addConnectorConfiguration(ConnectorConfiguration connectorConfiguration)
    {
        connectorConfigurations.add(connectorConfiguration);
        if (jadeContainer != null && jadeContainer.isStarted()) {
            addConnectorAgent(connectorConfiguration);
        }
    }

    /**
     * Init connector.
     */
    public void start()
    {
        logger.info("Starting Connector JADE container on {}:{}...", getJadeHost(), getJadePort());
        logger.info("Connecting to the JADE main container {}:{}...", getControllerHost(), getControllerPort());

        jadeContainer = Container.createContainer(
                getControllerHost(), getControllerPort(), getJadeHost(), getJadePort());
        jadeContainer.start();

        // start configured agents
        for (cz.cesnet.shongo.connector.api.ConnectorConfiguration configuration : connectorConfigurations) {
            addConnectorAgent(configuration);
        }
    }

    /**
     * Adds an {@link ConnectorAgent} for given {@code configuration} to the {@link #jadeContainer}.
     *
     * @param configuration
     */
    private void addConnectorAgent(ConnectorConfiguration configuration)
    {
        String agentName = configuration.getAgentName();
        jadeContainer.addAgent(agentName, ConnectorAgent.class, new Object[]{this.configuration, configuration});
    }

    /**
     * @param connectorAgentName
     * @return true when the agent exists, false otherwise
     */
    public boolean hasConnectorAgent(String connectorAgentName)
    {
        return jadeContainer.hasAgent(connectorAgentName);
    }

    private void runShell()
    {
        final Shell shell = new Shell();
        shell.setPrompt("connector");
        shell.setExitCommand("exit", "Shutdown the connector");
        shell.addCommands(ContainerCommandSet.createContainerCommandSet(jadeContainer));
        shell.addCommand("list", "List all connector agent instances", new CommandHandler()
        {
            @Override
            public void perform(CommandLine commandLine)
            {
                for (ConnectorConfiguration connectorConfiguration : connectorConfigurations) {
                    Shell.printInfo("Connector [%s]", connectorConfiguration.getAgentName());
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
                for (ConnectorConfiguration connectorConfiguration : connectorConfigurations) {
                    if (agentName.equals(connectorConfiguration.getAgentName())) {
                        shell.setPrompt(agentName + "@connector");
                        shell.addCommands(ConnectorContainerCommandSet.createContainerAgentCommandSet(
                                jadeContainer, agentName));
                        return;
                    }
                }
                Shell.printError("Agent [%s] was not found!", agentName);
            }
        });
        shell.run();
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
     * @return version of the {@link ConnectorContainer}
     */
    private static String getVersion()
    {
        String filename = "version.properties";
        Properties properties = new Properties();
        InputStream inputStream = ConnectorContainer.class.getClassLoader().getResourceAsStream(filename);
        if (inputStream == null) {
            throw new RuntimeException("Properties file '" + filename + "' was not found in the classpath.");
        }
        try {
            try {
                properties.load(inputStream);
            }
            finally {
                inputStream.close();
            }
        }
        catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return properties.getProperty("version");
    }

    /**
     * Main method of device connector.
     *
     * @param arguments
     */
    public static void main(String[] arguments) throws Exception
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
        Option optionDaemon = OptionBuilder.withLongOpt("daemon")
                .withDescription("Connector will be started as daemon without the interactive shell")
                .create("d");
        Options options = new Options();
        options.addOption(optionHost);
        options.addOption(optionPort);
        options.addOption(optionController);
        options.addOption(optionConfig);
        options.addOption(optionHelp);
        options.addOption(optionDaemon);

        // Parse command line
        CommandLine commandLine = null;
        try {
            CommandLineParser parser = new PosixParser();
            commandLine = parser.parse(options, arguments);
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
            System.setProperty(ConnectorContainerConfiguration.JADE_HOST, commandLine.getOptionValue(optionHost.getOpt()));
        }
        if (commandLine.hasOption(optionPort.getOpt())) {
            System.setProperty(ConnectorContainerConfiguration.JADE_PORT, commandLine.getOptionValue(optionPort.getOpt()));
        }
        if (commandLine.hasOption(optionController.getOpt())) {
            String url = commandLine.getOptionValue(optionController.getOpt());
            String[] urlParts = url.split(":");
            if (urlParts.length == 1) {
                System.setProperty(ConnectorContainerConfiguration.CONTROLLER_HOST, urlParts[0]);
            }
            else if (urlParts.length == 2) {
                System.setProperty(ConnectorContainerConfiguration.CONTROLLER_HOST, urlParts[0]);
                System.setProperty(ConnectorContainerConfiguration.CONTROLLER_PORT, urlParts[1]);
            }
            else {
                System.err.println("Failed to parse controller url. It should be in <HOST:URL> format.");
                System.exit(-1);
            }
        }

        // Prepare configuration file names
        List<String> configurationFileNames = new LinkedList<String>();
        if (commandLine.hasOption(optionConfig.getOpt())) {
            for (String configurationFilename : commandLine.getOptionValues(optionConfig.getOpt())) {
                if (!new File(configurationFilename).exists()) {
                    throw new IllegalArgumentException(
                            "Configuration file '" + configurationFilename + "' doesn't exist.");
                }
                configurationFileNames.add(configurationFilename);
            }
        }
        else {
            for (String configurationFilename : DEFAULT_CONFIGURATION_FILE_NAMES) {
                if (new File(configurationFilename).exists()) {
                    configurationFileNames.add(configurationFilename);
                }
            }
        }
        final ConnectorContainer connectorContainer = new ConnectorContainer(configurationFileNames);

        // Configure SSL
        ConfiguredSSLContext.getInstance().loadConfiguration(connectorContainer.getConfiguration());

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
                        Thread.sleep(connectorContainer.getControllerConnectionCheckPeriod().getMillis());
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        continue;
                    }
                    // We want to reconnect if container is not started or when the
                    // previous start failed
                    if (startFailed || connectorContainer.jadeContainer.isStarted() == false) {
                        logger.warn("Reconnecting to the JADE main container {}:{}...", connectorContainer.getControllerHost(),
                                connectorContainer.getControllerPort());
                        startFailed = false;
                        if (!connectorContainer.jadeContainer.start()) {
                            startFailed = true;
                        }
                    }
                }
            }
        });

        // Prepare shutdown runnable
        Runnable shutdown = new Runnable()
        {
            private boolean handled = false;

            public void run()
            {
                try {
                    if (handled) {
                        return;
                    }
                    logger.info("Shutdown has been started...");
                    logger.info("Stopping connector...");
                    connectThread.interrupt();
                    connectorContainer.stop();
                    Container.killAllJadeThreads();
                    logger.info("Shutdown successfully completed.");
                }
                catch (Exception exception) {
                    logger.error("Shutdown failed", exception);
                }
                finally {
                    handled = true;
                }
            }
        };

        // Run connector
        boolean shell = !commandLine.hasOption(optionDaemon.getOpt());
        try {
            connectorContainer.start();
            connectThread.start();
            logger.info("Connector successfully started.");

            // Configure shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(shutdown));

            if (shell) {
                // Run shell
                connectorContainer.runShell();
                // Shutdown
                shutdown.run();
            }
        }
        catch (Exception exception) {
            // Shutdown
            shutdown.run();
            throw exception;
        }
    }
}
