package cz.cesnet.shongo.jade;

import jade.content.AgentAction;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class UnknownAgentActionException extends RuntimeException
{
    /**
     * {@link AgentAction} which was requested to be executed but the agent doesn't know the action.
     */
    private AgentAction agentAction;

    /**
     * Constructor.
     *
     * @param agentAction sets the {@link #agentAction}
     */
    public UnknownAgentActionException(AgentAction agentAction)
    {
        this.agentAction = agentAction;
    }

    /**
     * @return {@link #agentAction}
     */
    public AgentAction getAgentAction()
    {
        return agentAction;
    }
}
