package cz.cesnet.shongo.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.util.IdentifiedChangeableObject;
import cz.cesnet.shongo.api.rpc.StructType;
import cz.cesnet.shongo.fault.TodoImplementException;
import jade.content.Concept;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a virtual room on a multipoint server device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Room extends IdentifiedChangeableObject implements StructType, Concept
{
    /**
     * Room name - set by users. Type: String
     */
    public static final String NAME = "name";

    /**
     * Room description - set by users. Type: String
     */
    public static final String DESCRIPTION = "description";

    /**
     * Set of {@link cz.cesnet.shongo.Technology}s for the room.
     */
    public static final String TECHNOLOGIES = "technologies";

    /**
     * Number of licenses to use for the room. Type: int
     */
    public static final String LICENSE_COUNT = "licenseCount";

    /**
     * Aliases of the room. Type: List<Alias>
     */
    public static final String ALIASES = "aliases";

    /**
     * Settings of the room. Type: List<RoomSetting>
     */
    public static final String ROOM_SETTINGS = "roomSettings";

    /**
     * Allowed participants of the room. Type: List<UserInformation>
     */
    public static final String PARTICIPANTS = "participants";

    /**
     * Constructor.
     */
    public Room()
    {
    }

    /**
     * @return name of the room (human-readable); might be <code>null</code> if no name has been assigned
     */
    public String getName()
    {
        return getPropertyStorage().getValue(NAME);
    }

    /**
     * @param name name of the room (human-readable); might be <code>null</code> to unset the room name
     */
    public void setName(String name)
    {
        getPropertyStorage().setValue(NAME, name);
    }

    /**
     * @return {@link #DESCRIPTION}
     */
    public String getDescription()
    {
        return getPropertyStorage().getValue(DESCRIPTION);
    }

    /**
     * @param description sets the {@link #DESCRIPTION}
     */
    public void setDescription(String description)
    {
        getPropertyStorage().setValue(DESCRIPTION, description);
    }

    /**
     * @return {@link #TECHNOLOGIES}
     */
    public Set<Technology> getTechnologies()
    {
        return getPropertyStorage().getCollection(TECHNOLOGIES, Set.class);
    }

    /**
     * @param technologies sets the {@link #TECHNOLOGIES}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        getPropertyStorage().setCollection(TECHNOLOGIES, technologies);
    }

    /**
     * @param technology technology to be added to the {@link #TECHNOLOGIES}
     */
    public void addTechnology(Technology technology)
    {
        getPropertyStorage().addCollectionItem(TECHNOLOGIES, technology, Set.class);
    }

    /**
     * @param technology technology to be removed from the {@link #TECHNOLOGIES}
     */
    public void removeTechnology(Technology technology)
    {
        getPropertyStorage().removeCollectionItem(TECHNOLOGIES, technology);
    }

    /**
     * @return number of ports that multipoint server can utilize for this room
     */
    public int getLicenseCount()
    {
        return getPropertyStorage().getValueAsInt(LICENSE_COUNT);
    }

    /**
     * @param licenseCount number of license that multipoint server can utilize for this room
     */
    public void setLicenseCount(int licenseCount)
    {
        if (licenseCount < 0) {
            throw new IllegalArgumentException("License count must be non-negative");
        }

        getPropertyStorage().setValue(LICENSE_COUNT, licenseCount);
    }

    /**
     * @return list of aliases under which the room is accessible
     */
    public List<Alias> getAliases()
    {
        return getPropertyStorage().getCollection(ALIASES, List.class);
    }

    /**
     * @return {@link Alias} for given {@code aliasType}
     */
    public Alias getAlias(AliasType aliasType)
    {
        for (Alias alias : this.getAliases()) {
            if (alias.getType() == aliasType) {
                return alias;
            }
        }
        return null;
    }

    /**
     * @param aliases aliases under which the room is accessible
     */
    public void setAliases(List<Alias> aliases)
    {
        getPropertyStorage().setCollection(ALIASES, aliases);
    }

    /**
     * Adds a new alias under which the room is accessible.
     *
     * @param alias alias to add for the room
     */
    public void addAlias(Alias alias)
    {
        getPropertyStorage().addCollectionItem(ALIASES, alias, List.class);
    }

    /**
     * @return {@link #ROOM_SETTINGS}
     */
    public List<RoomSetting> getRoomSettings()
    {
        return getPropertyStorage().getCollection(ROOM_SETTINGS, List.class);
    }

    /**
     * @param roomSettings sets the {@link #ROOM_SETTINGS}
     */
    public void setRoomSettings(List<RoomSetting> roomSettings)
    {
        getPropertyStorage().setCollection(ROOM_SETTINGS, roomSettings);
    }

    /**
     * @param roomSettingType
     * @return {@link RoomSetting} of given {@code roomSettingType} or null if doesn't exist
     */
    public <T extends RoomSetting> T getRoomSetting(Class<T> roomSettingType)
    {
        for (RoomSetting roomSetting : getRoomSettings()) {
            if (roomSettingType.isInstance(roomSetting)) {
                return roomSettingType.cast(roomSetting);
            }
        }
        return null;
    }

    /**
     * @param roomSetting to be added to the {@link #ROOM_SETTINGS}
     */
    public void addRoomSetting(RoomSetting roomSetting)
    {
        RoomSetting existingRoomSetting = getRoomSetting(roomSetting.getClass());
        if (existingRoomSetting != null) {
            throw new TodoImplementException("Merge same room setting");
        }
        else {
            getPropertyStorage().addCollectionItem(ROOM_SETTINGS, roomSetting, List.class);
        }
    }

    /**
     * @return {@link #PARTICIPANTS}
     */
    public List<UserInformation> getParticipants()
    {
        return getPropertyStorage().getCollection(PARTICIPANTS, List.class);
    }

    /**
     * @param participants sets the {@link #PARTICIPANTS}
     */
    public void setParticipants(List<UserInformation> participants)
    {
        getPropertyStorage().setCollection(PARTICIPANTS, participants);
    }

    /**
     * @param participant to be added to the {@link #PARTICIPANTS}
     */
    public void addParticipant(UserInformation participant)
    {
        getPropertyStorage().addCollectionItem(PARTICIPANTS, participant, List.class);
    }

    /**
     * @param participant to be removed from the {@link #PARTICIPANTS}
     */
    public void removeParticipant(UserInformation participant)
    {
        getPropertyStorage().removeCollectionItem(PARTICIPANTS, participant);
    }

    @Override
    public String toString()
    {
        return String.format(Room.class.getSimpleName() + " (id: %s, name: %s, description: %s, licenses: %d)",
                getId(), getName(), getDescription(), getLicenseCount());
    }
}
