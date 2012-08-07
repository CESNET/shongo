package cz.cesnet.shongo.controller.resource;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Capability tells that the device is able to host multiple virtual rooms.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class VirtualRoomsCapability extends DeviceCapability
{
    /**
     * Number of available ports.
     */
    private Integer portCount;

    /**
     * Contructor.
     */
    public VirtualRoomsCapability()
    {
    }

    /**
     * Constructor.
     *
     * @param portCount sets the {@link #portCount}
     */
    public VirtualRoomsCapability(Integer portCount)
    {
        this.portCount = portCount;
    }

    /**
     * @return {@link #portCount}
     */
    @Column
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
