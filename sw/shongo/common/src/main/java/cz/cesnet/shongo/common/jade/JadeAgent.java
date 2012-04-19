package cz.cesnet.shongo.common.jade;

import jade.core.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an agent in jade middle-ware.
 *
 * @author Martin Srom
 */
public class JadeAgent extends Agent
{
    private static Logger logger = LoggerFactory.getLogger(JadeAgent.class);

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
