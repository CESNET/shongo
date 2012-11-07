package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.util.IdentifiedObject;

/**
 * Represents an entity that can be scheduled.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceSummary extends IdentifiedObject
{
    /**
     * Name of the resource.
     */
    private String name;

    /**
     * Technologies of the resource.
     */
    private String technologies;

    /**
     * Parent resource identifier.
     */
    private String parentIdentifier;

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

    /**
     * @return {@link #technologies}
     */
    public String getTechnologies()
    {
        return technologies;
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    void setTechnologies(String technologies)
    {
        this.technologies = technologies;
    }

    /**
     * @return {@link #parentIdentifier}
     */
    public String getParentIdentifier()
    {
        return parentIdentifier;
    }

    /**
     * @param parentIdentifier sets the {@link #parentIdentifier}
     */
    public void setParentIdentifier(String parentIdentifier)
    {
        this.parentIdentifier = parentIdentifier;
    }
}
