package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;

/**
 * Capability tells that the device is able to host multiple virtual rooms.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomProviderCapability extends Capability
{
    /**
     * Number of available licenses.
     */
    public static final String LICENSE_COUNT = "licenseCount";

    /**
     * Constructor.
     */
    public RoomProviderCapability()
    {
    }

    /**
     * Constructor.
     *
     * @param licenseCount sets the {@link #LICENSE_COUNT}
     */
    public RoomProviderCapability(Integer licenseCount)
    {
        setLicenseCount(licenseCount);
    }

    /**
     * @return {@link #LICENSE_COUNT}
     */
    @Required
    public Integer getLicenseCount()
    {
        return getPropertyStorage().getValue(LICENSE_COUNT);
    }

    /**
     * @param licenseCount sets the {@link #LICENSE_COUNT}
     */
    public void setLicenseCount(Integer licenseCount)
    {
        getPropertyStorage().setValue(LICENSE_COUNT, licenseCount);
    }
}
