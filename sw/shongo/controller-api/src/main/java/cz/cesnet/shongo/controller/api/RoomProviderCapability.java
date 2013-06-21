package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.oldapi.annotation.Required;

import java.util.Set;

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
     * List of {@link AliasType} which are required for each created room.
     * If multiple technologies are supported by the owner {@link DeviceResource} and the room is created only
     * for a subset of the technologies, only alias {@link AliasType}s which are compatible with this technology subset
     * are required.
     */
    public static final String REQUIRED_ALIAS_TYPES = "requiredAliasTypes";

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
     * Constructor.
     *
     * @param licenseCount       sets the {@link #LICENSE_COUNT}
     * @param requiredAliasTypes sets the {@link #REQUIRED_ALIAS_TYPES}
     */
    public RoomProviderCapability(Integer licenseCount, AliasType[] requiredAliasTypes)
    {
        setLicenseCount(licenseCount);
        for (AliasType requiredAliasType : requiredAliasTypes) {
            addRequiredAliasType(requiredAliasType);
        }
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

    /**
     * @return {@link #REQUIRED_ALIAS_TYPES}
     */
    public Set<AliasType> getRequiredAliasTypes()
    {
        return getPropertyStorage().getCollection(REQUIRED_ALIAS_TYPES, Set.class);
    }

    /**
     * @param requiredAliasTypes sets the {@link #REQUIRED_ALIAS_TYPES}
     */
    public void setRequiredAliasTypes(Set<AliasType> requiredAliasTypes)
    {
        getPropertyStorage().setCollection(REQUIRED_ALIAS_TYPES, requiredAliasTypes);
    }

    /**
     * @param requiredAliasType to be added to the {@link #REQUIRED_ALIAS_TYPES}
     */
    public void addRequiredAliasType(AliasType requiredAliasType)
    {
        getPropertyStorage().addCollectionItem(REQUIRED_ALIAS_TYPES, requiredAliasType, Set.class);
    }

    /**
     * @param requiredAliasTypes to be removed from the {@link #REQUIRED_ALIAS_TYPES}
     */
    public void removeRequiredAliasType(AliasType requiredAliasTypes)
    {
        getPropertyStorage().removeCollectionItem(REQUIRED_ALIAS_TYPES, requiredAliasTypes);
    }
}
