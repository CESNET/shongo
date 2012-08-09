package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;

/**
 * Special type of requested resource for specific known resource.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExistingResourceSpecification extends ResourceSpecification
{
    /**
     * The resource identifier.
     */
    public static final String RESOURCE_IDENTIFIER = "resourceIdentifier";

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
