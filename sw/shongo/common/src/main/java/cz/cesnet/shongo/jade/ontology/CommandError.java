package cz.cesnet.shongo.jade.ontology;

import jade.content.Concept;

/**
 * A command result telling that the command should have work, but failed during execution.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CommandError implements Concept
{
    private String description;

    public CommandError()
    {
    }

    public CommandError(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
}
