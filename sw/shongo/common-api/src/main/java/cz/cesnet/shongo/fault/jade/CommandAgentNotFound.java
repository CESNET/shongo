package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.fault.CommonFault;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getMessage()}
 */
public class CommandAgentNotFound extends CommandFailure
{
    /**
     * Agent name for connector.
     */
    private String agentName;

    /**
     * Constructor.
     */
    private CommandAgentNotFound()
    {
    }

    /**
     * Constructor.
     *
     * @param agentName sets the {@link #agentName}
     */
    public CommandAgentNotFound(String agentName)
    {
        this.agentName = agentName;
    }

    /**
     * @return {@link #agentName}
     */
    public String getAgentName()
    {
        return agentName;
    }

    /**
     * @param agentName sets the {@link #agentName}
     */
    public void setAgentName(String agentName)
    {
        this.agentName = agentName;
    }

    @Override
    public int getCode()
    {
        return CommonFault.JADE_COMMAND_CONNECTOR_NOT_FOUND;
    }

    @Override
    public String getMessage()
    {
        return CommonFault.formatMessage("Jade agent '%s' was not found.", agentName);
    }
}
