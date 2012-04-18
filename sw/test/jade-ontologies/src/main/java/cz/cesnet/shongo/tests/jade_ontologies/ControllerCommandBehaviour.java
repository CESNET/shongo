package cz.cesnet.shongo.tests.jade_ontologies;

import cz.cesnet.shongo.tests.jade_ontologies.commands.Command;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.core.behaviours.CyclicBehaviour;

/**
 * Behaviour listening for and processing commands passed to the agent.
 *
 * A command is an instance of the <code>Command</code> class, specifying what to do. The Command object should be
 * passed to the agent by the means of Object-to-Agent communication - calling putO2AObject on the agent controller.
 *
 * An agent having this behaviour should have enabled the Object-to-Agent communication during its setup method:
 * <code>
 *     setEnabledO2ACommunication(true, 0);
 * </code>
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ControllerCommandBehaviour extends CyclicBehaviour
{
    @Override
    public void action()
    {
        Command command = (Command) myAgent.getO2AObject();
        if (command != null) {
            try {
                command.process(myAgent);
            }
            catch (Codec.CodecException e) {
                e.printStackTrace();
            }
            catch (OntologyException e) {
                e.printStackTrace();
            }
        }
        else {
            block();
        }
    }
}
