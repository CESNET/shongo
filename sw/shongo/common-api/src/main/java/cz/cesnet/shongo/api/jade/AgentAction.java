package cz.cesnet.shongo.api.jade;

import jade.content.onto.Ontology;

/**
 * Represents an {@link jade.content.AgentAction} for an {@link jade.core.Agent} with an {@link Ontology}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AgentAction implements jade.content.AgentAction
{
    /**
     * Unique identifier of the action which is automatically generated.
     */
    private Long id = null;

    /**
     * Used for generating {@link #id}.
     */
    private static Long lastGeneratedId = Long.valueOf(0);

    /**
     * @return {@link #id}
     */
    public Long getId()
    {
        synchronized (lastGeneratedId) {
            if (this.id == null) {
                this.id = ++lastGeneratedId;
            }
        }
        return this.id;
    }

    /**
     * @param id sets the {@link #id}
     */
    public void setId(Long id)
    {
        synchronized (lastGeneratedId) {
            if (id > lastGeneratedId) {
                lastGeneratedId = id;
            }
        }
        this.id = id;
    }

    /**
     * @return {@link Ontology} for this {@link AgentAction}
     */
    public abstract Ontology getOntology();

    @Override
    public String toString()
    {
        return getClass().getSimpleName();
    }
}
