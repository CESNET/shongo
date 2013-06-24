package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.Room;

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

    private static final String LICENSE_COUNT = "licenseCount";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(LICENSE_COUNT, licenseCount);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        licenseCount = dataMap.getInt(LICENSE_COUNT);
    }
}
