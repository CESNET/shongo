package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;

/**
 * {@link Specification} for existing endpoint {@link Resource}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExistingEndpointSpecification extends ParticipantSpecification
{
    /**
     * The resource identifier.
     */
    public static final String RESOURCE_IDENTIFIER = "resourceIdentifier";

    /**
     * Constructor.
     */
    public ExistingEndpointSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param resourceIdentifier sets the {@link #RESOURCE_IDENTIFIER}
     */
    public ExistingEndpointSpecification(String resourceIdentifier)
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
