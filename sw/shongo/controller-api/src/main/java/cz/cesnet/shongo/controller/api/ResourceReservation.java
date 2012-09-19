package cz.cesnet.shongo.controller.api;

/**
 * Represents a {@link Reservation} for a {@link Resource}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceReservation extends Reservation
{
    /**
     * Unique resourceIdentifier of the resource.
     */
    private String resourceIdentifier;

    /**
     * Name of the resource.
     */
    private String resourceName;

    /**
     * @return {@link #resourceIdentifier}
     */
    public String getResourceIdentifier()
    {
        return resourceIdentifier;
    }

    /**
     * @param resourceIdentifier sets the {@link #resourceIdentifier}
     */
    public void setIdentifier(String resourceIdentifier)
    {
        this.resourceIdentifier = resourceIdentifier;
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
