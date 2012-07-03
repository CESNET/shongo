package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.jade.command.Command;
import jade.core.behaviours.CyclicBehaviour;

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
    @Override
    public void action()
    {
        Agent agent = (Agent) myAgent;
        if (agent == null) {
            throw new RuntimeException("CommandBehaviour requires agent of class "
                    + Agent.class.getCanonicalName() + ".");
        }

        Object object = agent.getO2AObject();
        if (object == null) {
            block();
            return;
        }

        if (object instanceof Command) {
            Command command = (Command) object;
            command.process(agent);
        }
        else {
            throw new RuntimeException("CommandBehaviour can process only commands of class "
                    + Command.class.getCanonicalName() + " but "
                    + object.getClass().getCanonicalName() + " was presented.");
        }
    }
}
