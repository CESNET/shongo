package cz.cesnet.shongo.connector.common;

import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ConnectorConfiguration;
import cz.cesnet.shongo.connector.api.ConnectorInitException;
import cz.cesnet.shongo.connector.api.DeviceConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

import java.util.LinkedList;
import java.util.List;

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
     * Connector class.
     */
    private Class<? extends CommonService> connectorClass;

    /**
     * {@link DeviceConfiguration} for managed device.
     */
    private DeviceConfiguration deviceConfiguration;

    /**
     * Constructor.
     */
    public ConnectorConfigurationImpl(final DeviceAddress deviceAddress, final String userName, final String password)
    {
        configuration = new HierarchicalConfiguration();
        deviceConfiguration = new DeviceConfiguration()
        {
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
        };
    }

    /**
     * Constructor.
     *
     * @param configuration sets the {@link #configuration}
     */
    public ConnectorConfigurationImpl(final HierarchicalConfiguration configuration)
    {
        this.configuration = configuration;
        this.name = configuration.getString("name");
        String className = "cz.cesnet.shongo.connector.device." + configuration.getString("class");
        try {
            this.connectorClass = (Class<? extends CommonService>) Class.forName(className);
        }
        catch (ClassNotFoundException exception) {
            throw new ConnectorInitException("Connector class for " + name + " not found: " + className, exception);
        }
        if (AbstractDeviceConnector.class.isAssignableFrom(this.connectorClass)) {
            deviceConfiguration = new DeviceConfiguration() {
                @Override
                public DeviceAddress getAddress()
                {
                    String host = getStringRequired("host");
                    int port = configuration.getInteger("port", DeviceAddress.DEFAULT_PORT);
                    return new DeviceAddress(host, port);
                }

                @Override
                public String getUserName()
                {
                    return getStringRequired("auth.username");
                }

                @Override
                public String getPassword()
                {
                    return getStringRequired("auth.password");
                }
            };
        }
    }

    @Override
    public String getAgentName()
    {
        return name;
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
        return configuration.getString("options." + option);
    }

    @Override
    public List<cz.cesnet.shongo.connector.api.Configuration> getOptionConfigurationList(String option)
    {
        List<cz.cesnet.shongo.connector.api.Configuration> configurations =
                new LinkedList<cz.cesnet.shongo.connector.api.Configuration>();
        for (final HierarchicalConfiguration configuration : this.configuration.configurationsAt("options." + option)) {
            configurations.add(new cz.cesnet.shongo.connector.api.Configuration()
            {
                @Override
                public String getString(String attribute)
                {
                    return configuration.getString(attribute);
                }
            });
        }
        return configurations;
    }

    private String getStringRequired(String attribute)
    {
        String value = configuration.getString(attribute);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Attribute '" + attribute + "' must be set in connector configuration for '" + name + "'.");
        }
        return value;
    }
}
