package cz.cesnet.shongo.controller.resource;

import javax.persistence.Entity;

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
        if (resource != null && (resource instanceof DeviceResource) == false) {
            throw new IllegalArgumentException("Device capability can be inserted only to device resource!");
        }
        super.setResource(resource);
    }
}
