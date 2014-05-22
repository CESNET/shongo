package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.DataMap;

/**
 * Represents a managed mode for a device resource.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ManagedMode extends AbstractComplexType
{
    /**
     * Connector Jade agent name.
     */
    private String connectorAgentName;

    /**
     * Constructor.
     */
    public ManagedMode()
    {
    }

    /**
     * Constructor.
     *
     * @param connectorAgentName sets the {@link #connectorAgentName}
     */
    public ManagedMode(String connectorAgentName)
    {
        this.connectorAgentName = connectorAgentName;
    }

    /**
     * @return {@link #connectorAgentName}
     */
    public String getConnectorAgentName()
    {
        return connectorAgentName;
    }

    /**
     * @param connectorAgentName Sets the {@link #connectorAgentName}
     */
    public void setConnectorAgentName(String connectorAgentName)
    {
        this.connectorAgentName = connectorAgentName;
    }

    private static final String CONNECTOR_AGENT_NAME = "connectorAgentName";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(CONNECTOR_AGENT_NAME, connectorAgentName);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        connectorAgentName = dataMap.getStringRequired(CONNECTOR_AGENT_NAME, DEFAULT_COLUMN_LENGTH);
    }
}
