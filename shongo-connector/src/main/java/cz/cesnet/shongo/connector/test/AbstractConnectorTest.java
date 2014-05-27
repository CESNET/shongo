package cz.cesnet.shongo.connector.test;

import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.ConnectorContainer;
import cz.cesnet.shongo.connector.ConnectorScope;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ConnectorConfiguration;
import cz.cesnet.shongo.connector.api.DeviceConfiguration;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import cz.cesnet.shongo.connector.api.jade.ConnectorOntology;
import cz.cesnet.shongo.connector.common.AbstractConnector;
import cz.cesnet.shongo.connector.device.AdobeConnectConnector;
import cz.cesnet.shongo.controller.api.jade.ControllerOntology;
import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.jade.Container;
import cz.cesnet.shongo.jade.SendLocalCommand;
import cz.cesnet.shongo.util.Logging;
import jade.core.AID;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for testing connectors.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractConnectorTest
{
    private static Logger logger = LoggerFactory.getLogger(AbstractConnectorTest.class);

    /**
     * Constants for {@link cz.cesnet.shongo.connector.test.AbstractConnectorTest}..
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
     * Main JADE agent (used for sending commands to {@link cz.cesnet.shongo.connector.jade.ConnectorAgent}).
     */
    protected Agent mainAgent;

    /**
     * {@link cz.cesnet.shongo.connector.ConnectorContainer} with {@link cz.cesnet.shongo.connector.jade.ConnectorAgent}s.
     */
    protected ConnectorContainer connectorContainer;

    /**
     * Execute this {@link cz.cesnet.shongo.connector.test.AbstractConnectorTest}.
     */
    public final void execute()
    {
        before();
        run();
        after();
    }

    /**
     * Add new connector to {@link #connectorContainer}.
     *
     * @param agentName
     * @param connectorClass
     * @param deviceConfiguration
     * @param options
     */
    protected void addConnector(final String agentName, final Class<? extends AbstractConnector> connectorClass,
            final DeviceConfiguration deviceConfiguration, final Map<String, String> options)
    {
        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration()
        {
            @Override
            public String getAgentName()
            {
                return agentName;
            }

            @Override
            public Class<? extends CommonService> getConnectorClass()
            {
                return connectorClass;
            }

            @Override
            public DeviceConfiguration getDeviceConfiguration()
            {
                return deviceConfiguration;
            }

            @Override
            public String getOptionString(String option)
            {
                return options.get(option);
            }
        };
        addConnector(connectorConfiguration);
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
            for (AID agent : mainAgent.findAgentsByService(ConnectorScope.CONNECTOR_AGENT_SERVICE, 1000)) {
                if (agent.getLocalName().equals(connectorAgentName)) {
                    started = true;
                    break;
                }
            }
        }
    }

    /**
     * @param connectorAgentName
     * @param connectorCommand
     * @return result
     */
    protected <T> T performCommand(String connectorAgentName, ConnectorCommand<T> connectorCommand)
    {
        SendLocalCommand sendLocalCommand = mainAgent.sendCommand(connectorAgentName, connectorCommand);
        return (T) sendLocalCommand.getResult();
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
