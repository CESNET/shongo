package cz.cesnet.shongo.jade;

import jade.content.AgentAction;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class UnknownAgentActionException extends Throwable
{
    private AgentAction action;

    public UnknownAgentActionException(AgentAction action)
    {
        this.action = action;
    }
}
