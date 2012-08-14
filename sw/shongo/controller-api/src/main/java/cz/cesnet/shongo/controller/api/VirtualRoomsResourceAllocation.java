package cz.cesnet.shongo.controller.api;

/**
 * Represents an information about allocations of a resource.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class VirtualRoomsResourceAllocation extends ResourceAllocation
{
    /**
     * Maximum number of used ports.
     */
    private Integer maximumPortCount;

    /**
     * Number of available ports.
     */
    private Integer availablePortCount;

    /**
     * @return {@link #maximumPortCount}
     */
    public Integer getMaximumPortCount()
    {
        return maximumPortCount;
    }

    /**
     * @param maximumPortCount sets the {@link #maximumPortCount}
     */
    public void setMaximumPortCount(Integer maximumPortCount)
    {
        this.maximumPortCount = maximumPortCount;
    }

    /**
     * @return {@link #availablePortCount}
     */
    public Integer getAvailablePortCount()
    {
        return availablePortCount;
    }

    /**
     * @param availablePortCount sets the {@link #availablePortCount}
     */
    public void setAvailablePortCount(Integer availablePortCount)
    {
        this.availablePortCount = availablePortCount;
    }
}
