package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.controller.api.RoomProviderCapability;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public class RoomProviderCapabilityModel {

    private int licenseCount;

    private List<AliasType> requiredAliasTypes = new LinkedList<AliasType>();

    public RoomProviderCapabilityModel() {
    }

    public int getLicenseCount() {
        return licenseCount;
    }

    public void setLicenseCount(int licenseCount) {
        this.licenseCount = licenseCount;
    }

    public List<AliasType> getRequiredAliasTypes() {
        return requiredAliasTypes;
    }

    public void setRequiredAliasTypes(List<AliasType> requiredAliasTypes) {
        this.requiredAliasTypes = requiredAliasTypes;
    }

    public RoomProviderCapability toApi () {

        RoomProviderCapability roomProviderCapability = new RoomProviderCapability();
        roomProviderCapability.setLicenseCount(getLicenseCount());
        for (AliasType aliasType: getRequiredAliasTypes()) {
            roomProviderCapability.addRequiredAliasType(aliasType);
        }

        return roomProviderCapability;
    }
}
