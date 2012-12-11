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
     * The resource shongo-id.
     */
    public static final String RESOURCE_ID = "resourceId";

    /**
     * Constructor.
     */
    public ExistingEndpointSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param resourceId sets the {@link #RESOURCE_ID}
     */
    public ExistingEndpointSpecification(String resourceId)
    {
        setResourceId(resourceId);
    }

    /**
     * @return {@link #RESOURCE_ID}
     */
    @Required
    public String getResourceId()
    {
        return getPropertyStorage().getValue(RESOURCE_ID);
    }

    /**
     * @param resourceId sets the {@link #RESOURCE_ID}
     */
    public void setResourceId(String resourceId)
    {
        getPropertyStorage().setValue(RESOURCE_ID, resourceId);
    }
}
