package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.client.web.resource.ResourceCapacity;
import cz.cesnet.shongo.controller.api.RecordingCapability;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public class RecordingCapabilityModel extends DeviceCapability{

    private Integer licenseCount;

    public RecordingCapabilityModel() {
    }

    public Integer getLicenseCount() {
        return licenseCount;
    }

    public void setLicenseCount(Integer licenseCount) {
        this.licenseCount = licenseCount;
    }

    public RecordingCapability toApi() {
        RecordingCapability recordingCapability = new RecordingCapability();
        recordingCapability.setLicenseCount(getLicenseCount());
        return recordingCapability;
    }
}
