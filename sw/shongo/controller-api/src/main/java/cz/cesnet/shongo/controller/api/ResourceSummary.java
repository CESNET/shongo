package cz.cesnet.shongo.controller.api;

/**
 * Represents an entity that can be scheduled.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceSummary
{
    /**
     * Identifier of the resource.
     */
    private String identifier;

    /**
     * Name of the resource.
     */
    private String name;

    /**
     * @return {@link #identifier}
     */
    public String getIdentifier()
    {
        return identifier;
    }

    /**
     * @param identifier sets the {@link #identifier}
     */
    public void setIdentifier(String identifier)
    {
        this.identifier = identifier;
    }

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    void setName(String name)
    {
        this.name = name;
    }
}
