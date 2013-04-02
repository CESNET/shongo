package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;

/**
 * Represents an available {@link cz.cesnet.shongo.controller.common.RoomConfiguration} in
 * a {@link DeviceResource} with a
 * {@link cz.cesnet.shongo.controller.resource.RoomProviderCapability}.
 */
public class AvailableRoom
{
    /**
     * {@link DeviceResource} in which
     * the {@link cz.cesnet.shongo.controller.common.RoomConfiguration} is available.
     */
    private RoomProviderCapability roomProviderCapability;

    /**
     * Number of available licenses.
     */
    private int availableLicenseCount;

    /**
     * Maximum number of licenses which the device provides.
     */
    private int maximumLicenseCount;

    /**
     * @return {@link #roomProviderCapability}
     */
    public RoomProviderCapability getRoomProviderCapability()
    {
        return roomProviderCapability;
    }

    /**
     * @param roomProviderCapability sets the {@link #roomProviderCapability}
     */
    public void setRoomProviderCapability(RoomProviderCapability roomProviderCapability)
    {
        this.roomProviderCapability = roomProviderCapability;
    }

    /**
     * @return {@link DeviceResource} of the {@link #roomProviderCapability}
     */
    public DeviceResource getDeviceResource()
    {
        return roomProviderCapability.getDeviceResource();
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
