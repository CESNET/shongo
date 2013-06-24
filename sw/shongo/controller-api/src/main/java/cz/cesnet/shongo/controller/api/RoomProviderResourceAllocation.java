package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

/**
 * Represents an information about allocations of a resource.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomProviderResourceAllocation extends ResourceAllocation
{
    /**
     * Maximum number of used ports.
     */
    private int maximumLicenseCount;

    /**
     * Number of available ports.
     */
    private int availableLicenseCount;

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
     * @return {@link #availableLicenseCount}
     */
    public int getAvailableLicenseCount()
    {
        return availableLicenseCount;
    }

    /**
     * @param availableLicenseCount sets the {@link #availableLicenseCount}
     */
    public void setAvailableLicenseCount(int availableLicenseCount)
    {
        this.availableLicenseCount = availableLicenseCount;
    }

    private static final String MAXIMUM_LICENSE_COUNT = "maximumLicenseCount";
    private static final String AVAILABLE_LICENSE_COUNT = "availableLicenseCount";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(MAXIMUM_LICENSE_COUNT, maximumLicenseCount);
        dataMap.set(AVAILABLE_LICENSE_COUNT, availableLicenseCount);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        maximumLicenseCount = dataMap.getInt(MAXIMUM_LICENSE_COUNT);
        availableLicenseCount = dataMap.getInt(AVAILABLE_LICENSE_COUNT);
    }
}
