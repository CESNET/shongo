package cz.cesnet.shongo.tests.jade_ontologies.commands;

import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.core.Agent;

/**
 * The command interface for commands issued by the controller to the device agents.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface Command
{
    public void process(Agent processingAgent) throws Codec.CodecException, OntologyException;
}
