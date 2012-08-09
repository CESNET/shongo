package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.api.FaultException;
import cz.cesnet.shongo.controller.api.Capability;

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

    @Override
    public Capability toApi() throws FaultException
    {
        cz.cesnet.shongo.controller.api.VirtualRoomsCapability api =
                new cz.cesnet.shongo.controller.api.VirtualRoomsCapability();
        api.setId(getId().intValue());
        api.setPortCount(getPortCount());
        toApi(api);
        return api;
    }

    @Override
    public void fromApi(Capability api, EntityManager entityManager) throws FaultException
    {
        cz.cesnet.shongo.controller.api.VirtualRoomsCapability apiVirtualRoomsCapability =
                (cz.cesnet.shongo.controller.api.VirtualRoomsCapability) api;
        if (apiVirtualRoomsCapability.isPropertyFilled(apiVirtualRoomsCapability.PORT_COUNT)) {
            setPortCount(apiVirtualRoomsCapability.getPortCount());
        }
        super.fromApi(api, entityManager);
    }
}
