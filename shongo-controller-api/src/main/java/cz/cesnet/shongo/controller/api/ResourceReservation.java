package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

/**
 * Represents a {@link Reservation} for a {@link Resource}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceReservation extends Reservation
{
    /**
     * Shongo-id of the resource.
     */
    private String resourceId;

    /**
     * Name of the resource.
     */
    private String resourceName;

    /**
     * Description of the resource.
     */
    private String resourceDescription;

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

    /**
     * @return {@link #resourceName}
     */
    public String getResourceName()
    {
        return resourceName;
    }

    /**
     * @return {@link #resourceDescription}
     */
    public String getResourceDescription()
    {
        return resourceDescription;
    }

    /**
     * @param resourceDescription sets the {@link #resourceDescription}
     */
    public void setResourceDescription(String resourceDescription)
    {
        this.resourceDescription = resourceDescription;
    }

    /**
     * @param resourceName sets the {@link #resourceName}
     */
    public void setResourceName(String resourceName)
    {
        this.resourceName = resourceName;
    }

    private static final String RESOURCE_ID = "resourceId";
    private static final String RESOURCE_NAME = "resourceName";
    private static final String RESOURCE_DESCRIPTION = "resourceDescription";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESOURCE_ID, resourceId);
        dataMap.set(RESOURCE_NAME, resourceName);
        dataMap.set(RESOURCE_DESCRIPTION, resourceDescription);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        resourceId = dataMap.getString(RESOURCE_ID);
        resourceName = dataMap.getString(RESOURCE_NAME);
        resourceDescription = dataMap.getString(RESOURCE_DESCRIPTION);
    }
}
