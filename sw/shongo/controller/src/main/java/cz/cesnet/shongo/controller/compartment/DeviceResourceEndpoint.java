package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.controller.resource.DeviceResource;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;

/**
 * Represents a {@link DeviceResource} which acts as {@link Endpoint}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class DeviceResourceEndpoint extends Endpoint
{
    /**
     * {@link DeviceResource} which acts as {@link Endpoint}.
     */
    private DeviceResource deviceResource;

    /**
     * @return {@link #deviceResource}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public DeviceResource getDeviceResource()
    {
        return deviceResource;
    }

    /**
     * @param deviceResource sets the {@link #deviceResource}
     */
    public void setDeviceResource(DeviceResource deviceResource)
    {
        this.deviceResource = deviceResource;
    }
}
