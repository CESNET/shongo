package cz.cesnet.shongo.tests.jade_ontologies.ontology;

import jade.content.Concept;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class UserIdentity implements Concept
{
    private String id;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }
}
