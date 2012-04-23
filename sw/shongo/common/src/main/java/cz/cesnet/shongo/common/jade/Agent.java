package cz.cesnet.shongo.common.jade;

import cz.cesnet.shongo.common.jade.command.Command;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.wrapper.AgentController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an agent in JADE middle-ware.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Agent extends jade.core.Agent
{
    private static Logger logger = LoggerFactory.getLogger(Agent.class);

    /**
     * Is agent started?
     */
    private boolean started = false;

    /**
     * Is agent started?
     *
     * @return true if agent is started,
     *         false otherwise
     */
    public boolean isStarted()
    {
        return started;
    }

    /**
     * Register ontology that agent can handle.
     *
     * @param ontology
     */
    public void registerOntology(Ontology ontology)
    {
        getContentManager().registerOntology(ontology);
    }

    /**
     * Perform command on local agent
     *
     * @param command
     */
    public void performCommand(String agentName, Command command)
    {
        if (isStarted() == false) {
            logger.error("Cannot perform command when the agent is not started.");
            return;
        }
        try {
            this.putO2AObject(command, AgentController.SYNC);
        }
        catch (InterruptedException exception) {
            logger.error("Failed to put command object to agent queue.", exception);
        }

    }

    @Override
    protected void setup()
    {
        super.setup();

        started = true;

        logger.info("Agent [{}] is ready!", getAID().getName());

        // Register language
        getContentManager().registerLanguage(new SLCodec());

        // Agent will accept objects from container
        setEnabledO2ACommunication(true, 0);

        // Each agent is able to process commands and receive messages
        addBehaviour(new CommandBehaviour());
        addBehaviour(new ReceiverBehaviour());
    }

    @Override
    protected void takeDown()
    {
        started = false;

        super.takeDown();

        logger.info("Agent [{}] exiting!", getAID().getName());
    }
}
