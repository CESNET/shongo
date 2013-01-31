package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.fault.CommonFault;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getMessage()}
 */
public class CommandConnectorNotFound extends CommandFailure
{
    /**
     * Agent name for connector.
     */
    private String connectorAgentName;

    /**
     * Constructor.
     *
     * @param connectorAgentName sets the {@link #connectorAgentName}
     */
    public CommandConnectorNotFound(String connectorAgentName)
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
     * @param connectorAgentName sets the {@link #connectorAgentName}
     */
    public void setConnectorAgentName(String connectorAgentName)
    {
        this.connectorAgentName = connectorAgentName;
    }

    @Override
    public int getCode()
    {
        return CommonFault.JADE_COMMAND_CONNECTOR_NOT_FOUND;
    }

    @Override
    public String getMessage()
    {
        return CommonFault.formatMessage("Connector agent '%s' was not found.", connectorAgentName);
    }
}
