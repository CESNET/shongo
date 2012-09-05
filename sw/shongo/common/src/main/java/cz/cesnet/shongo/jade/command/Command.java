package cz.cesnet.shongo.jade.command;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.jade.Agent;

/**
 * Represents a command for a JADE agent.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface Command
{
    /**
     * Process this command on an agent.
     *
     * @param agent
     */
    public void process(Agent agent) throws CommandException, CommandUnsupportedException;
}
