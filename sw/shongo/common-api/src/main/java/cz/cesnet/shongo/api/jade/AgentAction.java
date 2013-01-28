package cz.cesnet.shongo.api.jade;

import jade.content.onto.Ontology;

/**
 * Represents an {@link jade.content.AgentAction} for an {@link jade.core.Agent} with an {@link Ontology}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface AgentAction extends jade.content.AgentAction
{
    /**
     * @return action unique identifier
     */
    public Long getId();

    /**
     * @param id sets the action unique identifier
     */
    public void setId(Long id);

    /**
     * @return {@link Ontology} for this {@link AgentAction}
     */
    public Ontology getOntology();
}
