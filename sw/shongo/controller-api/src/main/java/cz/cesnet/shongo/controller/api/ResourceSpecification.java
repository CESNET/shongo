package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;

/**
 * {@link Specification} for existing resource {@link Resource}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceSpecification extends Specification
{
    /**
     * The resource identifier.
     */
    public static final String RESOURCE_IDENTIFIER = "resourceIdentifier";

    /**
     * Constructor.
     */
    public ResourceSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param resourceIdentifier sets the {@link #RESOURCE_IDENTIFIER}
     */
    public ResourceSpecification(String resourceIdentifier)
    {
        setResourceIdentifier(resourceIdentifier);
    }

    /**
     * @return {@link #RESOURCE_IDENTIFIER}
     */
    @Required
    public String getResourceIdentifier()
    {
        return getPropertyStorage().getValue(RESOURCE_IDENTIFIER);
    }

    /**
     * @param resourceIdentifier sets the {@link #RESOURCE_IDENTIFIER}
     */
    public void setResourceIdentifier(String resourceIdentifier)
    {
        getPropertyStorage().setValue(RESOURCE_IDENTIFIER, resourceIdentifier);
    }
}
