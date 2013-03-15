package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.fault.old.CommonFault;

/**
 * Represents a {@link CommandFailure} which happens when a command should be sent from a sender agent
 * and that agent is not started.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommandAgentNotStarted extends CommandFailure
{
    /**
     * Agent name of Jade agent.
     */
    private String agentName;

    /**
     * Constructor.
     */
    private CommandAgentNotStarted()
    {
    }

    /**
     * Constructor.
     *
     * @param agentName sets the {@link #agentName}
     */
    public CommandAgentNotStarted(String agentName)
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
        return CommonFault.JADE_COMMAND_AGENT_NOT_STARTED;
    }

    @Override
    public String getMessage()
    {
        return CommonFault.formatMessage("Jade agent '%s' was not started yet.", agentName);
    }
}
