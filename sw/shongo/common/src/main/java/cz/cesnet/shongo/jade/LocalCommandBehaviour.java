package cz.cesnet.shongo.jade;

import jade.core.behaviours.CyclicBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Behaviour listening for and processing {@link LocalCommand}s passed to the agent.
 * <p/>
 * The {@link LocalCommand} object should be passed to the agent by the means of Object-to-Agent communication
 * (calling putO2AObject on the agent controller).
 * <p/>
 * An agent having this behaviour should have enabled the Object-to-Agent communication during its setup method:
 * <code>
 * setEnabledO2ACommunication(true, 0);
 * </code>
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class LocalCommandBehaviour extends CyclicBehaviour
{
    private static Logger logger = LoggerFactory.getLogger(LocalCommandBehaviour.class);

    /**
     * {@link Agent} which has this behavior.
     */
    private Agent agent;

    /**
     * Associates this behaviour with a Shongo agent.
     *
     * @param agent a Shongo agent (must be an instance of cz.cesnet.shongo.jade.Agent)
     */
    @Override
    public void setAgent(jade.core.Agent agent)
    {
        if (!(agent instanceof Agent)) {
            throw new IllegalArgumentException("This behaviour works only with instances of " + Agent.class);
        }

        this.agent = (Agent) agent;
        super.setAgent(agent);
    }

    @Override
    public void action()
    {
        Object object = agent.getO2AObject();
        if (object == null) {
            block();
            return;
        }
        if (!(object instanceof LocalCommand)) {
            throw new RuntimeException("CommandBehaviour can process only commands of class "
                    + LocalCommand.class.getCanonicalName() + " but "
                    + object.getClass().getCanonicalName() + " was presented.");
        }

        LocalCommand command = (LocalCommand) object;
        try {
            command.process(agent);
        }
        catch (LocalCommandException exception) {
            logger.error("Error processing the local command", exception);
        }
    }
}
