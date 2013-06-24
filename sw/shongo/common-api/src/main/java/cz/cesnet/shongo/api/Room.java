package cz.cesnet.shongo.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import jade.content.Concept;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Represents a virtual room on a multipoint server device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Room extends IdentifiedComplexType implements Concept
{
    /**
     * Room name - set by users. Type: String
     */
    private String name;

    /**
     * Room description - set by users. Type: String
     */
    private String description;

    /**
     * Set of {@link cz.cesnet.shongo.Technology}s for the room.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Number of licenses to use for the room. Type: int
     */
    private int licenseCount;

    /**
     * Aliases of the room. Type: List<Alias>
     */
    private List<Alias> aliases = new LinkedList<Alias>();

    /**
     * Settings of the room. Type: List<RoomSetting>
     */
    private List<RoomSetting> roomSettings = new LinkedList<RoomSetting>();

    /**
     * Allowed participants of the room. Type: List<UserInformation>
     */
    List<UserInformation> participants = new LinkedList<UserInformation>();

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
        return name;
    }

    /**
     * @param name name of the room (human-readable); might be <code>null</code> to unset the room name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #description}
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description sets the {@link #description}
     */
    public void setDescription(String description)
    {
        this.description = description;
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
     * @param technology technology to be added to the {@link #technologies}
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    /**
     * @param technology technology to be removed from the {@link #technologies}
     */
    public void removeTechnology(Technology technology)
    {
        technologies.remove(technology);
    }

    /**
     * @return number of ports that multipoint server can utilize for this room
     */
    public int getLicenseCount()
    {
        return licenseCount;
    }

    /**
     * @param licenseCount number of license that multipoint server can utilize for this room
     */
    public void setLicenseCount(int licenseCount)
    {
        if (licenseCount < 0) {
            throw new IllegalArgumentException("License count must be non-negative");
        }

        this.licenseCount = licenseCount;
    }

    /**
     * @return list of aliases under which the room is accessible
     */
    public List<Alias> getAliases()
    {
        return aliases;
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
        this.aliases = aliases;
    }

    /**
     * Adds a new alias under which the room is accessible.
     *
     * @param alias alias to add for the room
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
     * @param roomSetting to be added to the {@link #roomSettings}
     */
    public void addRoomSetting(RoomSetting roomSetting)
    {
        RoomSetting existingRoomSetting = getRoomSetting(roomSetting.getClass());
        if (existingRoomSetting != null) {
            throw new TodoImplementException("Merge same room setting");
        }
        else {
            roomSettings.add(roomSetting);
        }
    }

    /**
     * @return {@link #participants}
     */
    public List<UserInformation> getParticipants()
    {
        return participants;
    }

    /**
     * @param participants sets the {@link #participants}
     */
    public void setParticipants(List<UserInformation> participants)
    {
        this.participants = participants;
    }

    /**
     * @param participant to be added to the {@link #participants}
     */
    public void addParticipant(UserInformation participant)
    {
        String userId = participant.getUserId();
        for (UserInformation existingParticipant : getParticipants()) {
            if (userId.equals(existingParticipant.getUserId())) {
                // Participant is already added to the room
                return;
            }
        }
        this.participants.add(participant);
    }

    /**
     * @param participant to be removed from the {@link #participants}
     */
    public void removeParticipant(UserInformation participant)
    {
        participants.remove(participant);
    }

    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String TECHNOLOGIES = "technologies";
    public static final String LICENSE_COUNT = "licenseCount";
    public static final String ALIASES = "aliases";
    public static final String ROOM_SETTINGS = "roomSettings";
    public static final String PARTICIPANTS = "participants";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(NAME, name);
        dataMap.set(DESCRIPTION, description);
        dataMap.set(TECHNOLOGIES, technologies);
        dataMap.set(LICENSE_COUNT, licenseCount);
        dataMap.set(ALIASES, aliases);
        dataMap.set(ROOM_SETTINGS, roomSettings);
        dataMap.set(PARTICIPANTS, participants);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        name = dataMap.getString(NAME);
        description = dataMap.getString(DESCRIPTION);
        technologies = dataMap.getSet(TECHNOLOGIES, Technology.class);
        licenseCount = dataMap.getInt(LICENSE_COUNT);
        aliases = dataMap.getList(ALIASES, Alias.class);
        roomSettings = dataMap.getList(ROOM_SETTINGS, RoomSetting.class);
        participants = dataMap.getList(PARTICIPANTS, UserInformation.class);
    }

    @Override
    public String toString()
    {
        return String.format(Room.class.getSimpleName()
                + " (id: %s, name: %s, description: %s, licenses: %d, participants: %d)",
                getId(), getName(), getDescription(), getLicenseCount(), getParticipants().size());
    }
}
