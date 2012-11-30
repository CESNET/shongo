package cz.cesnet.shongo.controller.api;

/**
 * Represents an information about allocations of a resource.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class VirtualRoomsResourceAllocation extends ResourceAllocation
{
    /**
     * Maximum number of used ports.
     */
    private Integer maximumLicenseCount;

    /**
     * Number of available ports.
     */
    private Integer availableLicenseCount;

    /**
     * @return {@link #maximumLicenseCount}
     */
    public Integer getMaximumLicenseCount()
    {
        return maximumLicenseCount;
    }

    /**
     * @param maximumLicenseCount sets the {@link #maximumLicenseCount}
     */
    public void setMaximumLicenseCount(Integer maximumLicenseCount)
    {
        this.maximumLicenseCount = maximumLicenseCount;
    }

    /**
     * @return {@link #availableLicenseCount}
     */
    public Integer getAvailableLicenseCount()
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
}
