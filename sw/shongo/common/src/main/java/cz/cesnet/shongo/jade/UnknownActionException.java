package cz.cesnet.shongo.jade;

import jade.content.AgentAction;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class UnknownActionException extends Throwable
{
    private AgentAction action;

    public UnknownActionException(AgentAction action)
    {
        this.action = action;
    }
}
