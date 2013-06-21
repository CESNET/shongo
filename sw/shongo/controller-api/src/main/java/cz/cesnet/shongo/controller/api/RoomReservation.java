package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.oldapi.Room;

/**
 * Represents a {@link ResourceReservation} for a {@link Room}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomReservation extends ResourceReservation
{
    /**
     * License count.
     */
    private int licenseCount;

    /**
     * @return {@link #licenseCount}
     */
    public int getLicenseCount()
    {
        return licenseCount;
    }

    /**
     * @param licenseCount sets the {@link #licenseCount}
     */
    public void setLicenseCount(int licenseCount)
    {
        this.licenseCount = licenseCount;
    }
}
