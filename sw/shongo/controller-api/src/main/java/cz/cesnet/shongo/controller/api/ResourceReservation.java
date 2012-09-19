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
    private String name;

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
    public void setResourceIdentifier(String resourceIdentifier)
    {
        this.resourceIdentifier = resourceIdentifier;
    }

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
    public void setName(String name)
    {
        this.name = name;
    }
}
