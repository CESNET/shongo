package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.AliasType;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Capability tells that the {@link DeviceResource} can host one or more {@link cz.cesnet.shongo.controller.common.RoomConfiguration}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RoomProviderCapability extends DeviceCapability
{
    /**
     * Number of available ports.
     */
    private Integer licenseCount;

    /**
     * Set of {@link AliasType} which are required for each created room.
     * If multiple technologies are supported by the owner {@link DeviceResource} and the room is created only
     * for a subset of the technologies, only alias {@link AliasType}s which are compatible with this technology subset
     * are required.
     */
    private Set<AliasType> requiredAliasTypes = new HashSet<AliasType>();

    /**
     * Constructor.
     */
    public RoomProviderCapability()
    {
    }

    /**
     * Constructor.
     *
     * @param licenseCount sets the {@link #licenseCount}
     */
    public RoomProviderCapability(Integer licenseCount)
    {
        this.licenseCount = licenseCount;
    }

    /**
     * Constructor.
     *
     * @param licenseCount      sets the {@link #licenseCount}
     * @param requiredAliasType to be added to the {@link #requiredAliasTypes}
     */
    public RoomProviderCapability(Integer licenseCount, AliasType requiredAliasType)
    {
        this.licenseCount = licenseCount;
        this.requiredAliasTypes.add(requiredAliasType);
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

    /**
     * @return {@link #requiredAliasTypes}
     */
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<AliasType> getRequiredAliasTypes()
    {
        return Collections.unmodifiableSet(requiredAliasTypes);
    }

    /**
     * @param requiredAliasType to be added to the {@link #requiredAliasTypes}
     */
    public void addRequiredAliasType(AliasType requiredAliasType)
    {
        requiredAliasTypes.add(requiredAliasType);
    }

    /**
     * @param requiredAliasType to be removed from the {@link #requiredAliasTypes}
     */
    public void removeRequiredAliasType(AliasType requiredAliasType)
    {
        requiredAliasTypes.remove(requiredAliasType);
    }

    @Override
    public void loadLazyCollections()
    {
        super.loadLazyCollections();

        requiredAliasTypes.size();
    }

    @Override
    public cz.cesnet.shongo.controller.api.Capability createApi()
    {
        return new cz.cesnet.shongo.controller.api.RoomProviderCapability();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Capability api)
    {
        cz.cesnet.shongo.controller.api.RoomProviderCapability roomProviderCapabilityApi =
                (cz.cesnet.shongo.controller.api.RoomProviderCapability) api;
        roomProviderCapabilityApi.setId(getId());
        roomProviderCapabilityApi.setLicenseCount(getLicenseCount());
        for (AliasType requiredAliasType : getRequiredAliasTypes()) {
            roomProviderCapabilityApi.addRequiredAliasType(requiredAliasType);
        }
        super.toApi(api);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Capability api, EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.RoomProviderCapability roomProviderCapabilityApi =
                (cz.cesnet.shongo.controller.api.RoomProviderCapability) api;
        if (roomProviderCapabilityApi.isPropertyFilled(roomProviderCapabilityApi.LICENSE_COUNT)) {
            setLicenseCount(roomProviderCapabilityApi.getLicenseCount());
        }

        // Create required alias types
        for (AliasType requiredAliasType : roomProviderCapabilityApi.getRequiredAliasTypes()) {
            if (api.isPropertyItemMarkedAsNew(roomProviderCapabilityApi.REQUIRED_ALIAS_TYPES, requiredAliasType)) {
                addRequiredAliasType(requiredAliasType);
            }
        }
        // Delete required alias types
        Set<AliasType> aliasTypes = api.getPropertyItemsMarkedAsDeleted(roomProviderCapabilityApi.REQUIRED_ALIAS_TYPES);
        for (AliasType requiredAliasType : aliasTypes) {
            removeRequiredAliasType(requiredAliasType);
        }

        super.fromApi(api, entityManager);
    }
}
