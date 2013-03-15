package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.fault.old.CommonFault;

/**
 * Represents a {@link CommandFailure} which happens when an agent which is receiver of the command is not available.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
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
