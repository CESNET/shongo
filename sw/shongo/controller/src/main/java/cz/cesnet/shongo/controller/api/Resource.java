package cz.cesnet.shongo.controller.api;

/**
 * Represents a single resource
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Resource
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
     * Short name *
     */
    private String name;

    /**
     * Type of resource
     */
    private ResourceType type;

    /**
     * Type of technology
     */
    private TechnologyType technology;

    /**
     * Translation that can the resource perfomr
     */
    private Translation translation;

    /**
     * Long description
     */
    private String description;

    /**
     * Specifies whether resource can be allocated by scheduler
     */
    private boolean schedulable;

    /**
     * Specifies maximum future for reservations of the resource
     */
    //private DateTime maxFuture;

    /**
     * Child resources
     */
    private String[] resources;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getParentId()
    {
        return parentId;
    }

    public void setParentId(String parentId)
    {
        this.parentId = parentId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public ResourceType getType()
    {
        return type;
    }

    public void setType(ResourceType type)
    {
        this.type = type;
    }

    public TechnologyType getTechnology()
    {
        return technology;
    }

    public void setTechnology(TechnologyType technology)
    {
        this.technology = technology;
    }

    public Translation getTranslation()
    {
        return translation;
    }

    public void setTranslation(Translation translation)
    {
        this.translation = translation;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public boolean isSchedulable()
    {
        return schedulable;
    }

    public void setSchedulable(boolean schedulable)
    {
        this.schedulable = schedulable;
    }

    /*public DateTime getMaxFuture()
    {
        return maxFuture;
    }

    public void setMaxFuture(DateTime maxFuture)
    {
        this.maxFuture = maxFuture;
    }*/

    public String[] getResources()
    {
        return resources;
    }

    public void setResources(String[] resources)
    {
        this.resources = resources;
    }
}
