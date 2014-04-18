package cz.cesnet.shongo.tests.jade_ontologies.ontology;

import jade.content.Concept;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class SecurityToken implements Concept
{
    private UserIdentity user;

    public UserIdentity getUser()
    {
        return user;
    }

    public void setUser(UserIdentity user)
    {
        this.user = user;
    }
}
