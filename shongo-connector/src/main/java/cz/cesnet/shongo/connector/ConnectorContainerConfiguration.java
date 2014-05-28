package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.connector.api.ConnectorInitException;
import org.apache.commons.configuration.*;
import org.apache.commons.configuration.tree.NodeCombiner;
import org.apache.commons.configuration.tree.UnionCombiner;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Configuration for the {@link ConnectorContainer}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ConnectorContainerConfiguration extends CombinedConfiguration
{
    private static Logger logger = LoggerFactory.getLogger(ConnectorContainerConfiguration.class);

    /**
     * Configuration parameters names.
     */
    public static final String CONTROLLER_HOST = "controller.host";
    public static final String CONTROLLER_PORT = "controller.port";
    public static final String CONTROLLER_CONNECTION_CHECK_PERIOD = "controller.connection-check-period";
    public static final String JADE_HOST = "jade.host";
    public static final String JADE_PORT = "jade.port";

    Map<String, CombinedConfiguration> connectorConfigurationByName = new LinkedHashMap<String, CombinedConfiguration>();

    /**
     * Constructor.
     */
    public ConnectorContainerConfiguration()
    {
        this(new LinkedList<AbstractConfiguration>());
    }

    /**
     * Constructor.
     *
     * @param configuration
     */
    public ConnectorContainerConfiguration(final AbstractConfiguration configuration)
    {
        this(new LinkedList<AbstractConfiguration>()
        {{
                add(configuration);
            }});
    }

    /**
     * Constructor.
     *
     * @param configurations
     */
    public ConnectorContainerConfiguration(List<AbstractConfiguration> configurations)
    {
        NodeCombiner nodeCombiner = new UnionCombiner();
        nodeCombiner.addListNode("participant");
        setNodeCombiner(nodeCombiner);

        // System properties has the highest priority
        addConfiguration(new SystemConfiguration());
        // Added given configurations
        for (AbstractConfiguration configuration : configurations) {
            addConfiguration(configuration);
        }
        // Default configuration has the lowest priority
        try {
            XMLConfiguration xmlConfiguration = new XMLConfiguration();
            xmlConfiguration.setDelimiterParsingDisabled(true);
            xmlConfiguration.load(getClass().getClassLoader().getResource("connector-default.cfg.xml"));
            addConfiguration(xmlConfiguration);
        }
        catch (Exception exception) {
            throw new RuntimeException("Failed to load default connector configuration!", exception);
        }

        // Combine connector configurations by name
        for (HierarchicalConfiguration configuration : configurationsAt("connectors.connector")) {
            String connectorName = configuration.getString("name");
            if (connectorName == null) {
                throw new ConnectorInitException("Attribute name is required in connector configuration.");
            }
            CombinedConfiguration connectorConfiguration = connectorConfigurationByName.get(connectorName);
            if (connectorConfiguration == null) {
                connectorConfiguration = new CombinedConfiguration();
                connectorConfigurationByName.put(connectorName, connectorConfiguration);
            }
            connectorConfiguration.addConfiguration(configuration);
        }
    }

    /**
     * @return value from {@link #connectorConfigurationByName}
     */
    public Collection<CombinedConfiguration> getConnectorConfigurations()
    {
        return connectorConfigurationByName.values();
    }

    /**
     * @param connectorName
     * @return {@link CombinedConfiguration} for given {@code connectorName}
     */
    public CombinedConfiguration getConnectorConfiguration(String connectorName)
    {
        return connectorConfigurationByName.get(connectorName);
    }

    /**
     * @see {@link #getString(String)}
     */
    public Duration getDuration(String key)
    {
        String value = getString(key);
        if (value == null) {
            return null;
        }
        return Period.parse(value).toStandardDuration();
    }

    /**
     * @return timeout to receive response when performing commands from agent
     */
    public Duration getJadeCommandTimeout()
    {
        return getDuration("jade.command-timeout");
    }

    /**
     * @param configurationFileName
     * @return {@link AbstractConfiguration}
     */
    public static AbstractConfiguration loadConfiguration(String configurationFileName)
    {
        logger.info("Loading configuration from '{}'...", configurationFileName);
        try {
            File configurationFile = new File(configurationFileName);
            configurationFileName = configurationFile.getCanonicalPath();
            if (!configurationFile.exists()) {
                throw new RuntimeException("Configuration file '" + configurationFileName + "' doesn't exist.");
            }
            XMLConfiguration xmlConfiguration = new XMLConfiguration();
            xmlConfiguration.setDelimiterParsingDisabled(true);
            xmlConfiguration.load(configurationFileName);
            return xmlConfiguration;
        }
        catch (Exception exception) {
            throw new RuntimeException("Loading configuration file '" + configurationFileName + "' failed.", exception);
        }
    }

    /**
     * @param configurationFileNames
     * @return list of {@link AbstractConfiguration}
     */
    public static List<AbstractConfiguration> loadConfigurations(List<String> configurationFileNames)
    {
        List<AbstractConfiguration> configurations = new LinkedList<AbstractConfiguration>();
        for (String configurationFileName : configurationFileNames) {
            configurations.add(loadConfiguration(configurationFileName));
        }
        return configurations;
    }
}
