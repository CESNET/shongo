package cz.cesnet.shongo.common.jade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an agent in jade middle-ware.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Agent extends jade.core.Agent
{
    private static Logger logger = LoggerFactory.getLogger(Agent.class);

    @Override
    protected void setup()
    {
        logger.info("Agent [{}] is ready!", getAID().getName());
    }

    @Override
    protected void takeDown()
    {
        logger.info("Agent [{}] exiting!", getAID().getName());
    }
}
