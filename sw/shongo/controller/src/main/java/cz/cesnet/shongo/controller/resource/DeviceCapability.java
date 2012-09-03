package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.fault.FaultException;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
