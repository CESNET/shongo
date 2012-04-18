package cz.cesnet.shongo.controller;

import jade.core.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jade Agent for Domain Controller
 *
 * @author Martin Srom
 */
public class ControllerAgent extends Agent
{
    private static Logger logger = LoggerFactory.getLogger(ControllerAgent.class);

    @Override
    protected void setup()
    {
        logger.info("Controller Agent [{}] is ready!", getAID().getName());
    }

    @Override
    protected void takeDown()
    {
        logger.info("Controller Agent [{}] exiting!", getAID().getName());
    }
}
