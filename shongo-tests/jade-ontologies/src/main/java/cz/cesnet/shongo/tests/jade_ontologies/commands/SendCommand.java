package cz.cesnet.shongo.tests.jade_ontologies.commands;

import cz.cesnet.shongo.tests.jade_ontologies.ontology.ShongoOntology;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;

/**
 * A command for sending a concept to a single agent using the SL codec and Shongo ontology.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class SendCommand implements Command
{
    private AID recipient;
    private int performative;
    private Concept concept;

    public SendCommand(AID recipient, int performative, Concept concept)
    {
        this.recipient = recipient;
        this.performative = performative;
        this.concept = concept;
    }

    public AID getRecipient()
    {
        return recipient;
    }

    public void setRecipient(AID recipient)
    {
        this.recipient = recipient;
    }

    public int getPerformative()
    {
        return performative;
    }

    public void setPerformative(int performative)
    {
        this.performative = performative;
    }

    public Concept getConcept()
    {
        return concept;
    }

    public void setConcept(Concept concept)
    {
        this.concept = concept;
    }

    @Override
    public void process(Agent processingAgent) throws Codec.CodecException, OntologyException
    {
        ACLMessage msg = new ACLMessage(performative);
        msg.addReceiver(recipient);
        msg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
        msg.setOntology(ShongoOntology.NAME);

        ContentElement content = new Action(processingAgent.getAID(), getConcept());
        processingAgent.getContentManager().fillContent(msg, content);

        System.err.printf("%s -> %s: %s\n", processingAgent.getLocalName(), recipient.getLocalName(), msg.toString());
        processingAgent.send(msg);
    }
}
