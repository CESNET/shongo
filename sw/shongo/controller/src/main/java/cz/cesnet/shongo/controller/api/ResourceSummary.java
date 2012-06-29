package cz.cesnet.shongo.controller.api;

/**
 * Represents a summary of a resource
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceSummary
{
    /**
     * Unique identifier
     */
    private String id;

    /**
     * Parent resource identifier
     */
    private String parentId;

    /**
     * Short name
     */
    private String name;

    /**
     * Type of a resource
     */
    private ResourceType type;

    /**
     * Technology of a resource
     */
    private TechnologyType technology;

    /**
     * Specifies whether resource can be allocated by scheduler
     */
    private boolean schedulable;

    /**
     * Specifies maximum future for reservations of the resource
     */
    //private DateTime maxFuture;
    public String getId()
    {
        return id;
    }

    public String getParentId()
    {
        return parentId;
    }

    public String getName()
    {
        return name;
    }

    public ResourceType getType()
    {
        return type;
    }

    public TechnologyType getTechnology()
    {
        return technology;
    }

    public boolean isSchedulable()
    {
        return schedulable;
    }

    /*public DateTime getMaxFuture()
    {
        return maxFuture;
    }*/
}
