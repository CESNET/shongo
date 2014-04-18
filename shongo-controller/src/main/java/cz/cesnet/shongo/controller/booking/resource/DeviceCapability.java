package cz.cesnet.shongo.controller.booking.resource;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Represents a capability that
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class DeviceCapability extends Capability
{
    @Override
    public void setResource(Resource resource)
    {
        if (resource != null && !(resource instanceof DeviceResource)) {
            throw new IllegalArgumentException("Device capability can be inserted only into a device resource!");
        }
        super.setResource(resource);
    }

    /**
     * @return {@link #resource} as {@link DeviceResource}
     */
    @Transient
    public DeviceResource getDeviceResource()
    {
        return (DeviceResource) getResource();
    }
}
