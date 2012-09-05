package cz.cesnet.shongo.jade.command;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.jade.ontology.Message;
import cz.cesnet.shongo.jade.ontology.ShongoOntology;
import jade.content.AgentAction;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A command for a JADE agent to send a concept (agent action, message, ...) to an agent via JADE middleware.
 *
 * The SL codec and Shongo ontology is used to encode the message.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class SendCommand implements Command
{
    private static Logger logger = LoggerFactory.getLogger(SendCommand.class);

    /**
     * Message parameters
     */
    private AID recipient;
    private int performative;
    private Ontology ontology;
    private Concept concept;

    /**
     * Construct command that sends a concept to another agent.
     */
    public SendCommand(int performative)
    {
        this.performative = performative;
    }

    public static SendCommand createSendCommand(String performerName, AgentAction action)
    {
        AID agentId;
        if (performerName.contains("@")) {
            agentId = new AID(performerName, AID.ISGUID);
        }
        else {
            agentId = new AID(performerName, AID.ISLOCALNAME);
        }

        return createSendCommand(agentId, action);
    }

    /**
     * Create a command for sending an action request to another agent.
     *
     * @param performer    ID of agent who should perform the action
     * @param action       action to be performed
     * @return a prepared send command which, performed on an agent, sends the action request from this agent
     */
    public static SendCommand createSendCommand(AID performer, AgentAction action)
    {
        SendCommand sendCommand = new SendCommand(ACLMessage.INFORM);

        sendCommand.setRecipient(performer);
        sendCommand.setContent(ShongoOntology.getInstance(), action);

        return sendCommand;
    }

    /**
     * Create send command for sending simple text message
     *
     * @param agentName    name of agent to send the message to
     * @param message      text of the message
     * @return send command
     */
    public static SendCommand createSendMessage(String agentName, String message)
    {
        return createSendCommand(agentName, new Message(message));
    }

    /**
     * Get recipient.
     *
     * @return the intended recipient of the message
     */
    public AID getRecipient()
    {
        return recipient;
    }

    /**
     * Set recipient.
     *
     * @param recipient    the intended recipient of the message
     */
    public void setRecipient(AID recipient)
    {
        this.recipient = recipient;
    }

    /**
     * Get performative.
     *
     * @return performative
     */
    public int getPerformative()
    {
        return performative;
    }

    /**
     * Set performative
     *
     * @param performative
     */
    public void setPerformative(int performative)
    {
        this.performative = performative;
    }

    /**
     * Get content ontology.
     *
     * @return ontology
     */
    public Ontology getOntology()
    {
        return ontology;
    }

    /**
     * Get content concept
     *
     * @return concept
     */
    public Concept getConcept()
    {
        return concept;
    }

    /**
     * Set message content
     *
     * @param ontology
     * @param concept
     */
    public void setContent(Ontology ontology, Concept concept)
    {
        if (ontology == null) {
            throw new NullPointerException("ontology");
        }
        this.ontology = ontology;
        this.concept = concept;
    }

    @Override
    public void process(Agent agent) throws CommandException
    {
        if (concept == null) {
            throw new NullPointerException("concept is null, nothing to send");
        }
        if (ontology == null) {
            throw new NullPointerException("ontology is null, I do not know how to encode the message");
        }

        ACLMessage msg = new ACLMessage(performative);
        msg.addReceiver(recipient);
        msg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
        msg.setOntology(ontology.getName());
        msg.setSender(agent.getAID());

        ContentElement content = new Action(agent.getAID(), concept);
        try {
            agent.getContentManager().fillContent(msg, content);
        }
        catch (Codec.CodecException e) {
            throw new CommandException("Error in composing the command message.", e);
        }
        catch (OntologyException e) {
            throw new CommandException("Error in composing the command message.", e);
        }

        logger.info("{} -> {}: {}\n", new Object[]{agent.getAID().getName(), recipient.getName(), msg.toString()});
        agent.send(msg);
    }
}
