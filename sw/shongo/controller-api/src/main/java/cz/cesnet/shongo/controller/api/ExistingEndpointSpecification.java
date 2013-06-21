package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.oldapi.annotation.Required;

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
    private String resourceId;

    /**
     * Constructor.
     */
    public ExistingEndpointSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param resourceId sets the {@link #resourceId}
     */
    public ExistingEndpointSpecification(String resourceId)
    {
        setResourceId(resourceId);
    }

    /**
     * @return {@link #resourceId}
     */
    public String getResourceId()
    {
        return resourceId;
    }

    /**
     * @param resourceId sets the {@link #resourceId}
     */
    public void setResourceId(String resourceId)
    {
        this.resourceId = resourceId;
    }

    public static final String RESOURCE_ID = "resourceId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESOURCE_ID, resourceId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        resourceId = dataMap.getStringRequired(RESOURCE_ID);
    }
}
