package cz.cesnet.shongo.jade.command;

import cz.cesnet.shongo.jade.Agent;

/**
 * Represents a command that can be processed on a JADE agent.
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
    public boolean process(Agent agent);
}
