package cz.cesnet.shongo.controller.api;

/**
 * Represents a {@link ResourceReservation} for a virtual room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class VirtualRoomReservation extends ResourceReservation
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
