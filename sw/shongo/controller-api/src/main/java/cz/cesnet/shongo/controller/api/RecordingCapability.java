package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.DataMap;

import java.util.HashSet;
import java.util.Set;

/**
 * Capability tells that the device is able to record a call between endpoints.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RecordingCapability extends Capability
{
    /**
     * Number of allowed concurrent recordings ({@code null} means unlimited).
     */
    private Integer licenseCount;

    /**
     * Constructor.
     */
    public RecordingCapability()
    {
    }

    /**
     * Constructor.
     *
     * @param licenseCount sets the {@link #licenseCount}
     */
    public RecordingCapability(Integer licenseCount)
    {
        setLicenseCount(licenseCount);
    }

    /**
     * @return {@link #licenseCount}
     */
    public Integer getLicenseCount()
    {
        return licenseCount;
    }

    /**
     * @param licenseCount sets the {@link #licenseCount}
     */
    public void setLicenseCount(Integer licenseCount)
    {
        this.licenseCount = licenseCount;
    }

    public static final String LICENSE_COUNT = "licenseCount";

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
        licenseCount = dataMap.getInteger(LICENSE_COUNT);
    }
}
