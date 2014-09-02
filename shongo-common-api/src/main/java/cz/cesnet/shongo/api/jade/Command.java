package cz.cesnet.shongo.api.jade;

import jade.content.onto.Ontology;

/**
 * Represents an {@link jade.content.AgentAction} for performing a command on some {@link jade.core.Agent}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class Command implements jade.content.AgentAction
{
    /**
     * Unique identifier of the command which is automatically generated.
     */
    private Long id = null;

    /**
     * Used for generating {@link #id}.
     */
    private static Long lastGeneratedId = 0l;

    /**
     * @return {@link #id}
     */
    public Long getId()
    {
        synchronized (Command.class) {
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
        synchronized (Command.class) {
            if (id > lastGeneratedId) {
                lastGeneratedId = id;
            }
        }
        this.id = id;
    }

    /**
     * @return name of the command
     */
    public String getName()
    {
        return getClass().getSimpleName();
    }

    /**
     * @return {@link Ontology} for this {@link Command}
     */
    public abstract Ontology getOntology();

    @Override
    public String toString()
    {
        return getClass().getSimpleName();
    }
}
