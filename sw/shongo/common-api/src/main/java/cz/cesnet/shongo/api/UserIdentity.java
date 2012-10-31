package cz.cesnet.shongo.api;

import jade.content.Concept;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class UserIdentity implements Concept
{
    private String id;

    public UserIdentity(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }
}
