package cz.cesnet.shongo.connector.common;

import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ConnectorConfiguration;
import org.apache.commons.configuration.HierarchicalConfiguration;

/**
 * {@link ConnectorConfiguration} which is loaded from {@link HierarchicalConfiguration}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ConnectorConfigurationImpl extends ConnectorConfiguration
{
    /**
     * {@link HierarchicalConfiguration} from which the {@link ConnectorConfiguration} is loaded.
     */
    private HierarchicalConfiguration configuration;

    /**
     * Connector name.
     */
    private String name;

    /**
     * Constructor.
     *
     * @param configuration sets the {@link #configuration}
     */
    public ConnectorConfigurationImpl(HierarchicalConfiguration configuration)
    {
        this.configuration = configuration;
        this.name = configuration.getString("name");
    }

    @Override
    public String getAgentName()
    {
        return getStringRequired("name");
    }

    @Override
    public Class<? extends CommonService> getConnectorClass()
    {
        String className = "cz.cesnet.shongo.connector.device." + configuration.getString("class");
        try {
            return (Class<? extends CommonService>) Class.forName(className);
        }
        catch (ClassNotFoundException exception) {
            throw new RuntimeException("Connector class not found: " + className, exception);
        }
    }

    public DeviceAddress getDeviceAddress()
    {
        String host = getStringRequired("host");
        int port = configuration.getInteger("port", DeviceAddress.DEFAULT_PORT);
        return new DeviceAddress(host, port);
    }

    @Override
    public String getDeviceAuthUserName()
    {
        return getStringRequired("auth.username");
    }

    @Override
    public String getDeviceAuthPassword()
    {
        return getStringRequired("auth.password");
    }

    @Override
    public String getOptionString(String option)
    {
        return configuration.getString("options." + option);
    }

    @Override
    public String getOptionStringRequired(String option)
    {
        String value = getOptionString(option);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Option '" + option + "' must be set in connector '" + name + "' configuration.");
        }
        return value;
    }

    private String getStringRequired(String attribute)
    {
        String value = configuration.getString(attribute);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Attribute '" + attribute + "' must be set in connector '" + name + "' configuration.");
        }
        return value;
    }
}
