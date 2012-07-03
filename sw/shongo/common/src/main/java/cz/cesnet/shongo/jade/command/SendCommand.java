package cz.cesnet.shongo.jade.command;

import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.jade.ontology.Message;
import jade.content.Concept;
import jade.content.onto.Ontology;
import jade.core.AID;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Send message command for a JADE agent.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
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
     * Construct command that send message to another agent.
     */
    public SendCommand()
    {
    }

    /**
     * Create send command for sending simple text message
     *
     * @param agentName
     * @param message
     * @return send command
     */
    public static SendCommand createSendMessage(String agentName, String message)
    {
        SendCommand sendCommand = new SendCommand();

        AID agentId = null;
        if (agentName.indexOf("@") != -1) {
            agentId = new AID(agentName, AID.ISGUID);
        }
        else {
            agentId = new AID(agentName, AID.ISLOCALNAME);
        }
        sendCommand.setRecipient(agentId);
        sendCommand.setPerformative(ACLMessage.INFORM);
        sendCommand.setContent(null, new Message(message));

        return sendCommand;
    }

    /**
     * Get recipient.
     *
     * @return recipient
     */
    public AID getRecipient()
    {
        return recipient;
    }

    /**
     * Set recipient.
     *
     * @param recipient
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
        this.ontology = ontology;
        this.concept = concept;
    }

    @Override
    public boolean process(Agent agent)
    {
        ACLMessage msg = new ACLMessage(performative);
        msg.addReceiver(recipient);
        msg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
        if (ontology != null) {
            msg.setOntology(ontology.getName());
        }

        msg.setSender(agent.getAID());
        if (getConcept() instanceof Message) {
            msg.setContent(((Message) getConcept()).getMessage());
        }
        else {
            throw new RuntimeException("Concept " + getConcept().getClass().getCanonicalName() + " is not supported!");
        }

        /*ContentElement content = new Action(agent.getAID(), getConcept());
        try {
            agent.getContentManager().fillContent(msg, content);
        }
        catch (Codec.CodecException e) {
            e.printStackTrace();
            return false;
        }
        catch (OntologyException e) {
            e.printStackTrace();
            return false;
        }*/

        logger.info("{} -> {}: {}\n", new Object[]{agent.getAID().getName(), recipient.getName(), msg.toString()});
        agent.send(msg);

        return true;
    }
}
