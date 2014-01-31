package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.connector.api.ConnectorOptions;
import org.apache.commons.configuration.HierarchicalConfiguration;

import java.util.LinkedList;
import java.util.List;

/**
 * {@link ConnectorOptions} for {@link HierarchicalConfiguration}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ConfigurationConnectorOptions extends ConnectorOptions
{
    private final HierarchicalConfiguration configuration;

    public ConfigurationConnectorOptions(HierarchicalConfiguration configuration)
    {
        this.configuration = configuration;
    }

    @Override
    public String getString(String key)
    {
        return configuration.getString(key);
    }

    @Override
    public List<String> getStringList(String key)
    {
        List<String> stringList = new LinkedList<String>();
        for (String string : configuration.getStringArray(key)) {
            stringList.add(string);
        }
        return stringList;
    }

    @Override
    public List<ConnectorOptions> getOptionsList(String key)
    {
        List<ConnectorOptions> connectorOptions = new LinkedList<ConnectorOptions>();
        for (HierarchicalConfiguration configuration : this.configuration.configurationsAt(key)) {
            connectorOptions.add(new ConfigurationConnectorOptions(configuration));
        }
        return connectorOptions;
    }
}
