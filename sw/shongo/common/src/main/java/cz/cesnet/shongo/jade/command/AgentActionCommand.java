package cz.cesnet.shongo.jade.command;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.jade.AgentAction;
import cz.cesnet.shongo.jade.Agent;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Command} to send an {@link AgentAction} to an agent via JADE middle-ware.
 * <p/>
 * The SL codec and {@link cz.cesnet.shongo.api.jade.AgentAction#getOntology()} is used to encode the message.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class AgentActionCommand extends Command
{
    private static Logger logger = LoggerFactory.getLogger(AgentActionCommand.class);

    /**
     * Message parameters
     */
    private AgentAction action;
    private AID performer;

    /**
     * Construct command that sends a action to another agent.
     */
    public AgentActionCommand(String performerName, AgentAction action)
    {
        if (performerName.contains("@")) {
            performer = new AID(performerName, AID.ISGUID);
        }
        else {
            performer = new AID(performerName, AID.ISLOCALNAME);
        }

        this.action = action;
    }

    /**
     * @return {@link #action}
     */
    public AgentAction getAction()
    {
        return action;
    }

    @Override
    public String getName()
    {
        return action.getClass().getSimpleName();
    }

    @Override
    public void process(Agent agent) throws CommandException
    {
        ACLMessage initMsg = new ACLMessage(ACLMessage.REQUEST);
        initMsg.addReceiver(performer);
        initMsg.setSender(agent.getAID());
        initMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
        initMsg.setOntology(action.getOntology().getName());

        ContentElement content = new jade.content.onto.basic.Action(agent.getAID(), action);
        try {
            agent.getContentManager().fillContent(initMsg, content);
        }
        catch (Codec.CodecException e) {
            throw new CommandException("Error in composing the command message.", e);
        }
        catch (OntologyException e) {
            throw new CommandException("Error in composing the command message.", e);
        }

        logger.debug("{} initiating action request -> {}\n", agent.getAID().getName(), performer.getName());

        agent.addBehaviour(new AgentActionRequesterBehaviour(agent, initMsg, this));
        // FIXME: check that the behaviour is removed from the agent once it is done (or after some timeout)
    }
}
