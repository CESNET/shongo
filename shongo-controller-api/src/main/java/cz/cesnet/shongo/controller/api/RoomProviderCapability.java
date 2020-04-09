package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.DataMap;

import java.util.HashSet;
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
    private int licenseCount;

    /**
     * Maximum of licences per room.
     */
    private int maxLicencesPerRoom;

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
        setLicenseCount(licenseCount);
    }

    /**
     * Constructor.
     *
     * @param licenseCount       sets the {@link #licenseCount}
     * @param requiredAliasTypes sets the {@link #requiredAliasTypes}
     */
    public RoomProviderCapability(Integer licenseCount, AliasType[] requiredAliasTypes)
    {
        setLicenseCount(licenseCount);
        for (AliasType requiredAliasType : requiredAliasTypes) {
            addRequiredAliasType(requiredAliasType);
        }
    }

    /**
     * @return {@link #maxLicencesPerRoom}
     */
    public int getMaxLicencesPerRoom() {
        return maxLicencesPerRoom;
    }

    /**
     * @param maxLicencesPerRoom sets the {@link #maxLicencesPerRoom}
     */
    public void setMaxLicencesPerRoom(int maxLicencesPerRoom) {
        this.maxLicencesPerRoom = maxLicencesPerRoom;
    }

    /**
     * @return {@link #licenseCount}
     */
    public int getLicenseCount()
    {
        return licenseCount;
    }

    /**
     * @param licenseCount sets the {@link #licenseCount}
     */
    public void setLicenseCount(int licenseCount)
    {
        this.licenseCount = licenseCount;
    }

    /**
     * @return {@link #requiredAliasTypes}
     */
    public Set<AliasType> getRequiredAliasTypes()
    {
        return requiredAliasTypes;
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

    public static final String LICENSE_COUNT = "licenseCount";
    public static final String MAX_LICENCES_PER_ROOM = "maxLicencesPerRoom";
    public static final String REQUIRED_ALIAS_TYPES = "requiredAliasTypes";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(LICENSE_COUNT, licenseCount);
        dataMap.set(MAX_LICENCES_PER_ROOM, maxLicencesPerRoom);
        dataMap.set(REQUIRED_ALIAS_TYPES, requiredAliasTypes);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        licenseCount = dataMap.getInt(LICENSE_COUNT);
        maxLicencesPerRoom = dataMap.getInt(MAX_LICENCES_PER_ROOM);
        requiredAliasTypes = dataMap.getSet(REQUIRED_ALIAS_TYPES, AliasType.class);
    }
}
