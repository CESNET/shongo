package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.fault.FaultException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;

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
     * Constructor.
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

    @Override
    public cz.cesnet.shongo.controller.api.Capability createApi()
    {
        return new cz.cesnet.shongo.controller.api.VirtualRoomsCapability();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Capability api)
    {
        cz.cesnet.shongo.controller.api.VirtualRoomsCapability virtualRoomsCapability =
                (cz.cesnet.shongo.controller.api.VirtualRoomsCapability) api;
        virtualRoomsCapability.setIdentifier(getId());
        virtualRoomsCapability.setPortCount(getPortCount());
        super.toApi(api);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Capability api, EntityManager entityManager)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.VirtualRoomsCapability apiVirtualRoomsCapability =
                (cz.cesnet.shongo.controller.api.VirtualRoomsCapability) api;
        if (apiVirtualRoomsCapability.isPropertyFilled(apiVirtualRoomsCapability.PORT_COUNT)) {
            setPortCount(apiVirtualRoomsCapability.getPortCount());
        }
        super.fromApi(api, entityManager);
    }
}
