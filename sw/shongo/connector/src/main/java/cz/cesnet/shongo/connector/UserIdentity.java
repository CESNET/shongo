package cz.cesnet.shongo.connector;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class UserIdentity
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
