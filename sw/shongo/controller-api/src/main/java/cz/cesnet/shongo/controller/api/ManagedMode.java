package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.xmlrpc.StructType;

/**
 * Represents a managed mode for a device resource.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ManagedMode implements StructType
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
}
