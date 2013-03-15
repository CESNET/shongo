package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.fault.FaultSet;

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
    public String getMessage()
    {
        return FaultSet.formatMessage("Jade agent '%s' was not started yet.", agentName);
    }
}
