package cz.cesnet.shongo.controller.booking.recording;

import cz.cesnet.shongo.controller.booking.resource.DeviceCapability;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;

/**
 * Capability tells that the {@link cz.cesnet.shongo.controller.booking.resource.DeviceResource} can record a call.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RecordingCapability extends DeviceCapability
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
        this.licenseCount = licenseCount;
    }

    /**
     * @return {@link #licenseCount}
     */
    @Column
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

    @Override
    public cz.cesnet.shongo.controller.api.Capability createApi()
    {
        return new cz.cesnet.shongo.controller.api.RecordingCapability();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Capability api)
    {
        cz.cesnet.shongo.controller.api.RecordingCapability recordingCapabilityApi =
                (cz.cesnet.shongo.controller.api.RecordingCapability) api;
        recordingCapabilityApi.setLicenseCount(getLicenseCount());
        super.toApi(api);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Capability api, EntityManager entityManager)
    {
        super.fromApi(api, entityManager);

        cz.cesnet.shongo.controller.api.RecordingCapability recordingCapabilityApi =
                (cz.cesnet.shongo.controller.api.RecordingCapability) api;

        setLicenseCount(recordingCapabilityApi.getLicenseCount());
    }
}
