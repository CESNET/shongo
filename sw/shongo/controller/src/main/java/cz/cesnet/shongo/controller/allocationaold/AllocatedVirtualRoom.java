package cz.cesnet.shongo.controller.allocationaold;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.api.AllocatedItem;

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

    @Override
    protected AllocatedItem createApi()
    {
        return new cz.cesnet.shongo.controller.api.AllocatedVirtualRoom();
    }

    @Override
    protected void toApi(AllocatedItem api, Domain domain)
    {
        cz.cesnet.shongo.controller.api.AllocatedVirtualRoom apiAllocatedVirtualRoom =
                (cz.cesnet.shongo.controller.api.AllocatedVirtualRoom) api;
        apiAllocatedVirtualRoom.setPortCount(getPortCount());
        super.toApi(api, domain);
    }
}
