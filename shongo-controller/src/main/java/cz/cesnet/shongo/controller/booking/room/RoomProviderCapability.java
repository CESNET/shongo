package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.controller.api.Synchronization;
import cz.cesnet.shongo.controller.booking.resource.DeviceCapability;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Capability tells that the {@link DeviceResource} can host one or more {@link RoomEndpoint}s.
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
     * Maximum of licences per room.
     */
    private Integer maxLicencesPerRoom;

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
     * @param licenseCount sets the {@link #licenseCount}
     * @param maxLicencesPerRoom sets the {@link #maxLicencesPerRoom}
     *
     */
    public RoomProviderCapability(Integer licenseCount, Integer maxLicencesPerRoom)
    {
        this.licenseCount = licenseCount;
        this.maxLicencesPerRoom = maxLicencesPerRoom;
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
     * Constructor.
     *
     * @param licenseCount       sets the {@link #licenseCount}
     * @param requiredAliasTypes to be added to the {@link #requiredAliasTypes}
     */
    public RoomProviderCapability(Integer licenseCount, AliasType[] requiredAliasTypes)
    {
        this.licenseCount = licenseCount;
        for (AliasType requiredAliasType : requiredAliasTypes) {
            this.requiredAliasTypes.add(requiredAliasType);
        }
    }

    /**
     * @return {@link #maxLicencesPerRoom}
     */
    @Column
    public Integer getMaxLicencesPerRoom() {
        return maxLicencesPerRoom;
    }

    /**
     * @param maxLicencesPerRoom sets the {@link #maxLicencesPerRoom}
     */
    public void setMaxLicencesPerRoom(Integer maxLicencesPerRoom) {
        this.maxLicencesPerRoom = maxLicencesPerRoom;
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
    @Column(length = AbstractComplexType.ENUM_COLUMN_LENGTH)
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

    /**
     * @return true whether all allocated room are automatically recordable,
     *         false whether the recording must be explicitly allocated
     */
    @Transient
    public boolean isRoomRecordable()
    {
        DeviceResource deviceResource = getDeviceResource();
        RecordingCapability recordingCapability = deviceResource.getCapability(RecordingCapability.class);
        return recordingCapability != null && recordingCapability.getLicenseCount() == null;
    }

    @Override
    public void loadLazyProperties()
    {
        super.loadLazyProperties();

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
        roomProviderCapabilityApi.setMaxLicencesPerRoom(getMaxLicencesPerRoom());
        for (AliasType requiredAliasType : getRequiredAliasTypes()) {
            roomProviderCapabilityApi.addRequiredAliasType(requiredAliasType);
        }
        super.toApi(api);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Capability api, EntityManager entityManager)
    {
        super.fromApi(api, entityManager);

        cz.cesnet.shongo.controller.api.RoomProviderCapability roomProviderCapabilityApi =
                (cz.cesnet.shongo.controller.api.RoomProviderCapability) api;

        setLicenseCount(roomProviderCapabilityApi.getLicenseCount());
        setMaxLicencesPerRoom(roomProviderCapabilityApi.getMaxLicencesPerRoom());

        Synchronization.synchronizeCollection(requiredAliasTypes, roomProviderCapabilityApi.getRequiredAliasTypes());
    }
}
