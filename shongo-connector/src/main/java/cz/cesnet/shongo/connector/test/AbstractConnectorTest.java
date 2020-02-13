package cz.cesnet.shongo.connector.test;

import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.ConnectorContainer;
import cz.cesnet.shongo.connector.ConnectorContainerConfiguration;
import cz.cesnet.shongo.connector.ConnectorScope;
import cz.cesnet.shongo.connector.api.ConnectorConfiguration;
import cz.cesnet.shongo.connector.api.DeviceConfiguration;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import cz.cesnet.shongo.connector.api.jade.ConnectorOntology;
import cz.cesnet.shongo.connector.common.ConnectorConfigurationImpl;
import cz.cesnet.shongo.connector.jade.ConnectorAgent;
import cz.cesnet.shongo.controller.ControllerScope;
import cz.cesnet.shongo.controller.api.jade.ControllerOntology;
import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.jade.Container;
import cz.cesnet.shongo.jade.SendLocalCommand;
import cz.cesnet.shongo.ssl.ConfiguredSSLContext;
import cz.cesnet.shongo.util.Logging;
import jade.core.AID;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.mutable.MutableInt;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Class for testing connectors.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractConnectorTest
{
    protected static Logger logger = LoggerFactory.getLogger(AbstractConnectorTest.class);

    /**
     * @see ConnectorContainer#DEFAULT_CONFIGURATION_FILE_NAMES
     */
    public static final List<String> DEFAULT_CONFIGURATION_FILE_NAMES =
            Arrays.asList(ConnectorContainer.DEFAULT_CONFIGURATION_FILE_NAMES);

    /**
     * Constants for {@link AbstractConnectorTest}..
     */
    private static final String JADE_MAIN_HOST = "127.0.0.1";
    private static final int JADE_MAIN_PORT = 8282;
    private static final String JADE_SLAVE_HOST = "127.0.0.1";
    private static final int JADE_SLAVE_PORT = 8383;
    private static final String JADE_MAIN_PLATFORM_ID = "Shongo-Test";
    private static final String JADE_MAIN_AGENT_NAME = "Test";

    /**
     * Main JADE container.
     */
    protected Container mainContainer;

    /**
     * Main JADE agent (used for sending commands to {@link ConnectorAgent}).
     */
    protected Agent mainAgent;

    /**
     * {@link ConnectorContainer} with {@link ConnectorAgent}s.
     */
    protected ConnectorContainer connectorContainer;

    /**
     * @see ConnectorContainerConfiguration
     */
    protected ConnectorContainerConfiguration containerConfiguration;

    /**
     * Constructor.
     *
     * @param configurationFileNames
     */
    protected AbstractConnectorTest(String directory, List<String> configurationFileNames)
    {
        List<AbstractConfiguration> configurations = new LinkedList<AbstractConfiguration>();
        for (String configurationFileName : configurationFileNames) {
            String configurationFilePath = directory + "/" + configurationFileName;
            if (!new File(configurationFilePath).exists()) {
                String newDirectory = directory.replaceFirst("\\.\\./", "");
                String newConfigurationFilePath = configurationFilePath;
                while (!newDirectory.equals(directory)) {
                    directory = newDirectory;
                    logger.info("Configuration file '" + newConfigurationFilePath + "' doesn't exist. Trying parent directory...");
                    newConfigurationFilePath = newDirectory + "/" + configurationFileName;
                    if (new File(newConfigurationFilePath).exists()) {
                        configurationFilePath = newConfigurationFilePath;
                        break;
                    }
                    newDirectory = directory.replaceFirst("\\.\\./", "");
                }
            }
            if (new File(configurationFilePath).exists()) {
                configurations.add(ConnectorContainerConfiguration.loadConfiguration(configurationFilePath));
            }
            else {
                logger.warn("Configuration file '" + configurationFilePath + "' doesn't exist.");
            }
        }
        this.containerConfiguration = new ConnectorContainerConfiguration(configurations);
    }

    /**
     * @param connectorName
     * @return {@link ConnectorConfiguration} for given {@code connectorName}
     */
    public ConnectorConfiguration getConnectorConfiguration(String connectorName)
    {
        CombinedConfiguration connectorConfiguration = containerConfiguration.getConnectorConfiguration(connectorName);
        if (connectorConfiguration == null) {
            throw new IllegalArgumentException(
                    "Configuration for '" + connectorName + "' doesn't exist.");
        }
        if (connectorConfiguration.getString("class") == null) {
            throw new IllegalArgumentException(
                    "Configuration for '" + connectorName + "' doesn't define connector class.");
        }
        return new ConnectorConfigurationImpl(connectorConfiguration);
    }

    /**
     * Execute this {@link AbstractConnectorTest}.
     */
    public final void execute()
    {
        before();
        run();
        after();
    }

    /**
     * @param connectorName
     */
    protected Connector addConnector(String connectorName)
    {
        ConnectorConfiguration connectorConfiguration = getConnectorConfiguration(connectorName);
        addConnector(connectorConfiguration);
        return new Connector(connectorConfiguration);
    }

    /**
     * @param connectorConfiguration to be added to the {@link #connectorContainer}
     */
    protected void addConnector(ConnectorConfiguration connectorConfiguration)
    {
        connectorContainer.addConnectorConfiguration(connectorConfiguration);

        // Wait for connector agent to become visible
        String connectorAgentName = connectorConfiguration.getAgentName();
        boolean started = false;
        while (!started) {
            if(!connectorContainer.hasConnectorAgent(connectorAgentName)) {
                throw new RuntimeException("Connector '" + connectorAgentName + "' failed to start.");
            }
            for (AID agent : mainAgent.findAgentsByService(ConnectorScope.CONNECTOR_AGENT_SERVICE, 1000)) {
                if (agent.getLocalName().equals(connectorAgentName)) {
                    started = true;
                    break;
                }
            }
        }
    }

    /**
     * @param connector
     * @param connectorCommand
     * @return result
     */
    protected <T> T performCommand(Connector connector, ConnectorCommand<T> connectorCommand)
    {
        SendLocalCommand sendLocalCommand = mainAgent.sendCommand(connector.getName(), connectorCommand);
        if (sendLocalCommand.getState().equals(SendLocalCommand.State.SUCCESSFUL)) {
            return (T) sendLocalCommand.getResult();
        }
        else {
            throw new RuntimeException(sendLocalCommand.getJadeReport().toString());
        }
    }

    /**
     * @param duration
     */
    protected void sleep(Duration duration)
    {
        try {
            Thread.sleep(duration.getMillis());
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.warn("Sleep interrupted", exception);
        }
    }

    /**
     * @param message
     */
    protected void waitForUserCheck(String message)
    {
        javax.swing.JOptionPane.showMessageDialog(
                null, message, "Waiting", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * @param command
     * @param sender
     * @return result
     */
    protected Object handleControllerCommand(Command command, AID sender)
    {
        return null;
    }

    /**
     * @param object to be dumped
     */
    public void dump(Object object)
    {
        System.err.println(dumpString(object));
    }

    /**
     * @param object to be dumped
     */
    private String dumpString(Object object)
    {
        if (object == null) {
            return "null";
        }
        else if (object instanceof String) {
            return (String) object;
        }
        else if (object instanceof Collection) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Object item : (Collection) object) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append(",\n");
                }
                String itemString = dumpString(item);
                itemString = "  " + itemString.replace("\n", "\n  ");
                stringBuilder.append(itemString);
            }
            return "[\n" + stringBuilder + "\n]";
        }
        else {
            return ReflectionToStringBuilder.toString(object, ToStringStyle.MULTI_LINE_STYLE);
        }
    }

    /**
     * Before the test body is executed.
     */
    @Before
    public void before()
    {
        logger.info("Starting Connector Test...");

        Logging.installBridge();

        // Configure SSL
        ConfiguredSSLContext.getInstance().loadConfiguration(containerConfiguration);

        // Start main JADE container
        logger.info("Starting main JADE container on {}:{} (platform {})...",
                new Object[]{JADE_MAIN_HOST, JADE_MAIN_PORT, JADE_MAIN_PLATFORM_ID});
        this.mainContainer = Container.createMainContainer(JADE_MAIN_HOST, JADE_MAIN_PORT, JADE_MAIN_PLATFORM_ID);
        if (!mainContainer.start()) {
            throw new RuntimeException("Failed to start main JADE container.");
        }
        this.mainAgent = mainContainer.addAgent(JADE_MAIN_AGENT_NAME, new Agent(){
            @Override
            protected void setup()
            {
                // Configure this agent
                addOntology(ConnectorOntology.getInstance());
                addOntology(ControllerOntology.getInstance());
                super.setup();
            }
        }, null);
        // Create controller agent
        this.mainContainer.addAgent("Controller", new Agent(){
            @Override
            protected void setup()
            {
                addOntology(ConnectorOntology.getInstance());
                addOntology(ControllerOntology.getInstance());
                super.setup();
                registerService(ControllerScope.CONTROLLER_AGENT_SERVICE,
                        ControllerScope.CONTROLLER_AGENT_SERVICE_NAME);
            }

            @Override
            public Object handleCommand(Command command, AID sender) throws CommandException
            {
                return handleControllerCommand(command, sender);
            }
        }, null);

        // Start connector
        HierarchicalConfiguration configuration = new HierarchicalConfiguration();
        configuration.setProperty("controller.host", JADE_MAIN_HOST);
        configuration.setProperty("controller.port", JADE_MAIN_PORT);
        configuration.setProperty("jade.host", JADE_SLAVE_HOST);
        configuration.setProperty("jade.port", JADE_SLAVE_PORT);
        this.connectorContainer = new ConnectorContainer(configuration);
        this.connectorContainer.start();
    }

    /**
     * Run test body.
     */
    public void run()
    {
    }

    /**
     * After the test body is executed.
     */
    @After
    public void after()
    {
        // Stop connector
        this.connectorContainer.stop();

        // Stop main JADE container
        logger.info("Stopping main JADE container...");
        this.mainContainer.stop();
        logger.info("Killing remaining JADE threads...");
        Container.killAllJadeThreads();

        logger.info("Connector Test finished.");
    }

    private MutableInt level = new MutableInt();

    private Stack<MutableInt> levels = new Stack<MutableInt>();

    protected void printTestBegin(Connector connector, String name)
    {
        this.level.increment();

        StringBuilder message = new StringBuilder();
        for (MutableInt level : this.levels) {
            message.append(level);
            message.append("");
        }
        message.append(level);
        message.append("");
        message.append(" Testing '");
        message.append(connector.getName());
        message.append("': ");
        message.append(name);
        message.append("...\n");
        message.append("--------------------------------------------------------------------------------");
        if (this.levels.size() == 0) {
            logger.info("");
        }
        logger.info(message.toString());

        this.levels.push(this.level);
        this.level = new MutableInt(0);
    }

    protected void printTestEnd(Connector connector)
    {
        this.level = this.levels.pop();
        if (this.levels.size() == 0) {
            StringBuilder message = new StringBuilder();
            for (MutableInt level : this.levels) {
                message.append(level);
                message.append("");
            }
            message.append(level);
            message.append("");
            message.append(" Testing '");
            message.append(connector.getName());
            message.append("' finished.\n");
            message.append("--------------------------------------------------------------------------------");
            logger.info(message.toString());
        }
    }

    public static class Connector
    {
        private ConnectorConfiguration configuration;

        public Connector(ConnectorConfiguration configuration)
        {
            this.configuration = configuration;
        }

        public String getName()
        {
            return configuration.getAgentName();
        }

        public String getAddress()
        {
            return "https://" + configuration.getDeviceConfiguration().getAddress().getHost();
        }
    }

    /**
     * Simple implementation of {@link DeviceConfiguration}.
     */
    public static class Device implements DeviceConfiguration
    {
        private DeviceAddress deviceAddress;

        private String userName;

        private String password;

        public Device(String host, int port, String userName, String password)
        {
            this.deviceAddress = new DeviceAddress(host, port);
            this.userName = userName;
            this.password = password;
        }

        @Override
        public DeviceAddress getAddress()
        {
            return deviceAddress;
        }

        @Override
        public String getUserName()
        {
            return userName;
        }

        @Override
        public String getPassword()
        {
            return password;
        }
    }
}
