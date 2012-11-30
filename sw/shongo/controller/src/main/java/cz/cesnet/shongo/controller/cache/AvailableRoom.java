package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.controller.resource.DeviceResource;

/**
 * Represents an available {@link cz.cesnet.shongo.controller.common.Room} in
 * a {@link cz.cesnet.shongo.controller.resource.DeviceResource} with a
 * {@link cz.cesnet.shongo.controller.resource.RoomProviderCapability}.
 */
public class AvailableRoom
{
    /**
     * {@link cz.cesnet.shongo.controller.resource.DeviceResource} in which
     * the {@link cz.cesnet.shongo.controller.common.Room} is available.
     */
    private DeviceResource deviceResource;

    /**
     * Number of available licenses.
     */
    private int availableLicenseCount;

    /**
     * Maximum number of licenses which the device provides.
     */
    private int maximumLicenseCount;

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
     * @return {@link #availableLicenseCount}
     */
    public int getAvailableLicenseCount()
    {
        return availableLicenseCount;
    }

    /**
     * @param availableLicenseCount sets the {@link #availableLicenseCount}
     */
    public void setAvailableLicenseCount(Integer availableLicenseCount)
    {
        this.availableLicenseCount = availableLicenseCount;
    }

    /**
     * @return {@link #maximumLicenseCount}
     */
    public int getMaximumLicenseCount()
    {
        return maximumLicenseCount;
    }

    /**
     * @param maximumLicenseCount sets the {@link #maximumLicenseCount}
     */
    public void setMaximumLicenseCount(int maximumLicenseCount)
    {
        this.maximumLicenseCount = maximumLicenseCount;
    }

    /**
     * @return ratio of fullness for the device (0.0 - 1.0)
     */
    public double getFullnessRatio()
    {
        return 1.0 - (double) availableLicenseCount / (double) maximumLicenseCount;
    }
}
