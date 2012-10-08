package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.jade.command.Command;
import jade.core.behaviours.CyclicBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Behaviour listening for and processing commands passed to the agent.
 * <p/>
 * A command is an instance of the <code>Command</code> class, specifying what to do. The Command object should be
 * passed to the agent by the means of Object-to-Agent communication - calling putO2AObject on the agent controller.
 * <p/>
 * An agent having this behaviour should have enabled the Object-to-Agent communication during its setup method:
 * <code>
 * setEnabledO2ACommunication(true, 0);
 * </code>
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CommandBehaviour extends CyclicBehaviour
{
    private static Logger logger = LoggerFactory.getLogger(CommandBehaviour.class);

    private Agent myShongoAgent;

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

        myShongoAgent = (Agent) agent;
        super.setAgent(agent);
    }

    @Override
    public void action()
    {
        Object object = myShongoAgent.getO2AObject();
        if (object == null) {
            block();
            return;
        }

        if (!(object instanceof Command)) {
            throw new RuntimeException("CommandBehaviour can process only commands of class "
                    + Command.class.getCanonicalName() + " but "
                    + object.getClass().getCanonicalName() + " was presented.");
        }

        Command command = (Command) object;
        try {
            command.process(myShongoAgent);
        }
        catch (CommandException e) {
            logger.error("Error processing the command", e);
        }
    }
}
