package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.controller.common.Room;
import cz.cesnet.shongo.fault.FaultException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;

/**
 * Capability tells that the {@link DeviceResource} can host one or more {@link Room}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RoomProviderCapability extends DeviceCapability
{
    /**
     * Number of available ports.
     */
    private Integer licenseCount;

    /**
     * Constructor.
     */
    public RoomProviderCapability()
    {
    }

    /**
     * Constructor.
     *
     * @param licenseCount sets the {@link #licenseCount}
     */
    public RoomProviderCapability(Integer licenseCount)
    {
        this.licenseCount = licenseCount;
    }

    /**
     * @return {@link #licenseCount}
     */
    @Column
    public Integer getLicenseCount()
    {
        return licenseCount;
    }

    /**
     * @param licenseCount sets the {@link #licenseCount}
     */
    public void setLicenseCount(Integer licenseCount)
    {
        this.licenseCount = licenseCount;
    }

    @Override
    public cz.cesnet.shongo.controller.api.Capability createApi()
    {
        return new cz.cesnet.shongo.controller.api.RoomProviderCapability();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Capability api)
    {
        cz.cesnet.shongo.controller.api.RoomProviderCapability virtualRoomsCapability =
                (cz.cesnet.shongo.controller.api.RoomProviderCapability) api;
        virtualRoomsCapability.setIdentifier(getId());
        virtualRoomsCapability.setLicenseCount(getLicenseCount());
        super.toApi(api);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Capability api, EntityManager entityManager)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.RoomProviderCapability apiVirtualRoomsCapability =
                (cz.cesnet.shongo.controller.api.RoomProviderCapability) api;
        if (apiVirtualRoomsCapability.isPropertyFilled(apiVirtualRoomsCapability.LICENSE_COUNT)) {
            setLicenseCount(apiVirtualRoomsCapability.getLicenseCount());
        }
        super.fromApi(api, entityManager);
    }
}
