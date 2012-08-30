package cz.cesnet.shongo.controller.scheduler.plan;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.VirtualRoomsCapability;

import java.util.Set;

/**
 * Represents a virtual room in a scheduler plan.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class VirtualRoom extends Endpoint
{
    /**
     * Device resource with {@link VirtualRoomsCapability}.
     */
    private DeviceResource deviceResource;

    /**
     * Port count used for the virtual room.
     */
    private int portCount;

    /**
     * Constructor.
     *
     * @param deviceResource sets the {@link #deviceResource}
     * @param portCount      sets the {@link #portCount}
     */
    public VirtualRoom(DeviceResource deviceResource, int portCount)
    {
        if (!deviceResource.hasCapability(VirtualRoomsCapability.class)) {
            throw new IllegalArgumentException("Device resource must have VirtualRooms capability!");
        }
        this.deviceResource = deviceResource;
        this.portCount = portCount;
    }

    @Override
    public Set<Technology> getSupportedTechnologies()
    {
        return deviceResource.getCapabilityTechnologies(VirtualRoomsCapability.class);
    }
}
