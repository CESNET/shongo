package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.controller.booking.resource.DeviceResource;

/**
 * Represents an available {@link RoomEndpoint} in a {@link DeviceResource} with a {@link RoomProviderCapability}.
 */
public final class AvailableRoom
{
    /**
     * {@link DeviceResource} in which the {@link RoomConfiguration} is available.
     */
    private final RoomProviderCapability roomProviderCapability;

    /**
     * Number of available {@link RoomProviderCapability#licenseCount}.
     */
    private final int availableLicenseCount;

    /**
     * Number of maximum licences per room.
     */
    private final int maxLicencesPerRoom;

    /**
     * Constructor.
     *
     * @param roomProviderCapability sets the {@link #roomProviderCapability}
     * @param usedLicenseCount to be used for computing {@link #availableLicenseCount}
     */
    public AvailableRoom(RoomProviderCapability roomProviderCapability, int usedLicenseCount)
    {
        this.roomProviderCapability = roomProviderCapability;
        this.availableLicenseCount = roomProviderCapability.getLicenseCount() - usedLicenseCount;
        if (this.availableLicenseCount < 0) {
            throw new IllegalStateException("Available license count can't be negative.");
        }
        if (roomProviderCapability.getMaxLicencesPerRoom() != null) {
            this.maxLicencesPerRoom = roomProviderCapability.getMaxLicencesPerRoom();
        } else {
            //if not set - no restrictions
            this.maxLicencesPerRoom = 0;

        }

    }

    /**
     * @return {@link #roomProviderCapability}
     */
    public RoomProviderCapability getRoomProviderCapability()
    {
        return roomProviderCapability;
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
     * @return maximum {@link RoomProviderCapability#licenseCount} for {@link #roomProviderCapability}
     */
    public Integer getMaximumLicenseCount()
    {
        return roomProviderCapability.getLicenseCount();
    }

    /**
     * @return maximum {@link RoomProviderCapability#licenseCount} for {@link #roomProviderCapability}
     */
    public Integer getMaxLicencesPerRoom()
    {
        return maxLicencesPerRoom;
    }


    /**
     * @return ratio of fullness for the device (0.0 - 1.0)
     */
    public Double getFullnessRatio()
    {
        return 1.0 - (double) getAvailableLicenseCount() / (double) getMaximumLicenseCount();
    }
}
