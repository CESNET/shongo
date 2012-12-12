package cz.cesnet.shongo.controller.api;

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
     * @param resourceName sets the {@link #resourceName}
     */
    public void setResourceName(String resourceName)
    {
        this.resourceName = resourceName;
    }
}
