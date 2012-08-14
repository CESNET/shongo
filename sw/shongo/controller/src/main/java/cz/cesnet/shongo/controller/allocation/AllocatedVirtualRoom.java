package cz.cesnet.shongo.controller.allocation;

import javax.persistence.Entity;

/**
 * Represents a special type of {@link AllocatedDevice} an allocated virtual room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AllocatedVirtualRoom extends AllocatedDevice
{
    /**
     * Allocated port count.
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
     * @param portCount {@link #portCount}
     */
    public void setPortCount(Integer portCount)
    {
        this.portCount = portCount;
    }
}
