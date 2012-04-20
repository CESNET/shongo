package cz.cesnet.shongo.common.jade.command;

import cz.cesnet.shongo.common.jade.Agent;
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

import javax.naming.OperationNotSupportedException;

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
     *
     * @param recipient
     * @param performative
     * @param ontology
     * @param concept
     */
    public SendCommand(AID recipient, int performative, Ontology ontology, Concept concept)
    {
        setRecipient(recipient);
        setPerformative(performative);
        setContent(ontology, concept);
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
        msg.setOntology(ontology.getName());

        ContentElement content = new Action(agent.getAID(), getConcept());
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
        }

        logger.info("{} -> {}: {}\n", new Object[]{agent.getLocalName(), recipient.getLocalName(), msg.toString()});
        agent.send(msg);

        return true;
    }
}
