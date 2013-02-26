package cz.cesnet.shongo.api.jade;

import jade.content.onto.Ontology;

/**
 * {@link AgentAction}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PingAgentAction extends AgentAction
{
    @Override
    public Ontology getOntology()
    {
        return CommonOntology.getInstance();
    }
}
