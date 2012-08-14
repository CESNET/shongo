package cz.cesnet.shongo.controller.api;

/**
 * Special type of {@link AllocatedResource} which represents an allocated virtual room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocatedVirtualRoom extends AllocatedResource
{
    /**
     * Number of ports available for the virtual room.
     */
    private Integer portCount;

    /**
     * @return {@link #portCount}
     */
    public Integer getPortCount()
    {
        return portCount;
    }

    /**
     * @param portCount sets the {@link #portCount}
     */
    public void setPortCount(Integer portCount)
    {
        this.portCount = portCount;
    }
}
