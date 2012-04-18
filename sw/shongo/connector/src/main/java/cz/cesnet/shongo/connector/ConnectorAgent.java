package cz.cesnet.shongo.connector;

import jade.core.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jade Agent for Device Connector
 *
 * @author Martin Srom
 */
public class ConnectorAgent extends Agent
{
    private static Logger logger = LoggerFactory.getLogger(ConnectorAgent.class);

    @Override
    protected void setup()
    {
        logger.info("Connector Agent [{}] is ready!", getAID().getName());
    }

    @Override
    protected void takeDown()
    {
        logger.info("Connector Agent [{}] exiting!", getAID().getName());
    }
}
