package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.RoomSetting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a virtual room which interconnects
 * {@link cz.cesnet.shongo.controller.api.EndpointExecutable}s in
 * the {@link cz.cesnet.shongo.controller.api.CompartmentExecutable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomExecutable extends Executable
{
    /**
     * Id of the {@link Resource}.
     */
    private String resourceId;

    /**
     * Technology specific room identifier.
     */
    private String roomId;

    /**
     * Set of technologies which the virtual room shall support.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * License count.
     */
    private int licenseCount;

    /**
     * List of assigned {@link cz.cesnet.shongo.api.Alias}es to the {@link cz.cesnet.shongo.controller.api.EndpointExecutable}.
     */
    private List<Alias> aliases = new ArrayList<Alias>();

    /**
     * List of {@link cz.cesnet.shongo.api.RoomSetting}s for the {@link cz.cesnet.shongo.controller.api.RoomExecutable}.
     */
    private List<RoomSetting> roomSettings = new ArrayList<RoomSetting>();

    /**
     * @return {@link #resourceId}
     */
    public String getResourceId()
    {
        return resourceId;
    }

    /**
     * @param resourceId sets the {@link #resourceId}
     */
    public void setResourceId(String resourceId)
    {
        this.resourceId = resourceId;
    }

    /**
     * @return {@link #roomId}
     */
    public String getRoomId()
    {
        return roomId;
    }

    /**
     * @param roomId sets the {@link #roomId}
     */
    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }

    /**
     * @return {@link #technologies}
     */
    public Set<Technology> getTechnologies()
    {
        return technologies;
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies = technologies;
    }

    /**
     * Clear {@link #technologies}.
     */
    public void clearTechnologies()
    {
        technologies.clear();
    }

    /**
     * @param technology technology to be added to the set of technologies that the device support.
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
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
     * @return {@link #aliases}
     */
    public List<Alias> getAliases()
    {
        return aliases;
    }

    /**
     * @param aliasType
     * @return first {@link Alias} of given {@code aliasType}
     */
    public Alias getAliasByType(AliasType aliasType)
    {
        for (Alias alias : aliases) {
            if (alias.getType().equals(aliasType)) {
                return alias;
            }
        }
        return null;
    }

    /**
     * @param aliases sets the {@link #aliases}
     */
    public void setAliases(List<Alias> aliases)
    {
        this.aliases = aliases;
    }

    /**
     * Clear {@link #aliases}.
     */
    public void clearAliases()
    {
        aliases.clear();
    }

    /**
     * @param alias to be added to the {@link #aliases}
     */
    public void addAlias(Alias alias)
    {
        aliases.add(alias);
    }

    /**
     * @return {@link #roomSettings}
     */
    public List<RoomSetting> getRoomSettings()
    {
        return roomSettings;
    }

    /**
     * @param roomSettings sets the {@link #roomSettings}
     */
    public void setRoomSettings(List<RoomSetting> roomSettings)
    {
        this.roomSettings = roomSettings;
    }

    /**
     * Clear {@link #roomSettings}.
     */
    public void clearRoomSettings()
    {
        roomSettings.clear();
    }

    /**
     * @param roomSetting to be added to the {@link #roomSettings}
     */
    public void addRoomSetting(RoomSetting roomSetting)
    {
        roomSettings.add(roomSetting);
    }

    private static final String RESOURCE_ID = "resourceId";
    private static final String ROOM_ID = "roomId";
    private static final String TECHNOLOGIES = "technologies";
    private static final String LICENSE_COUNT = "licenseCount";
    private static final String ALIASES = "aliases";
    private static final String ROOM_SETTINGS = "roomSettings";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESOURCE_ID, resourceId);
        dataMap.set(ROOM_ID, roomId);
        dataMap.set(TECHNOLOGIES, technologies);
        dataMap.set(LICENSE_COUNT, licenseCount);
        dataMap.set(ALIASES, aliases);
        dataMap.set(ROOM_SETTINGS, roomSettings);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        resourceId = dataMap.getString(RESOURCE_ID);
        roomId = dataMap.getString(ROOM_ID);
        technologies = dataMap.getSet(TECHNOLOGIES, Technology.class);
        licenseCount = dataMap.getInt(LICENSE_COUNT);
        aliases = dataMap.getList(ALIASES, Alias.class);
        roomSettings = dataMap.getList(ROOM_SETTINGS, RoomSetting.class);
    }
}
