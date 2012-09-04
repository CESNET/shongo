package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.controller.resource.DeviceResource;

/**
 * Represents an available virtual room(s) in a {@link cz.cesnet.shongo.controller.resource.DeviceResource} with a {@link cz.cesnet.shongo.controller.resource.VirtualRoomsCapability}.
 */
public class AvailableVirtualRoom
{
    /**
     * {@link cz.cesnet.shongo.controller.resource.DeviceResource} in which the virtual room(s) is/are available.
     */
    private DeviceResource deviceResource;

    /**
     * Number of available ports.
     */
    private int availablePortCount;

    /**
     * Maximum number of ports which the device provides.
     */
    private int maximumPortCount;

    /**
     * @return {@link #deviceResource}
     */
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

    /**
     * @return {@link #availablePortCount}
     */
    public int getAvailablePortCount()
    {
        return availablePortCount;
    }

    /**
     * @param availablePortCount sets the {@link #availablePortCount}
     */
    public void setAvailablePortCount(Integer availablePortCount)
    {
        this.availablePortCount = availablePortCount;
    }

    /**
     * @return {@link #maximumPortCount}
     */
    public int getMaximumPortCount()
    {
        return maximumPortCount;
    }

    /**
     * @param maximumPortCount sets the {@link #maximumPortCount}
     */
    public void setMaximumPortCount(int maximumPortCount)
    {
        this.maximumPortCount = maximumPortCount;
    }

    /**
     * @return ratio of fullness for the device (0.0 - 1.0)
     */
    public double getFullnessRatio()
    {
        return 1.0 - (double) availablePortCount / (double) maximumPortCount;
    }
}
