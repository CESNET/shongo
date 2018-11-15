package cz.cesnet.shongo.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.Technology;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Represents a meeting room on a multipoint server device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Room extends IdentifiedComplexType
{
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
     * Default room layout for participants.
     */
    private RoomLayout layout;

    /**
     * Settings of the room. Type: List<RoomSetting>
     */
    private List<RoomSetting> roomSettings = new LinkedList<RoomSetting>();

    /**
     * Configuration of participants who has access to this room.
     */
    List<RoomParticipantRole> participantRoles = new LinkedList<RoomParticipantRole>();

    /**
     * Constructor.
     */
    public Room()
    {
    }

    /**
     * Constructor.
     */
    public Room(String roomId)
    {
        this.id = roomId;
    }

    /**
     * @return {@link Alias#value} for {@link AliasType#ROOM_NAME}
     */
    public String getName()
    {
        Alias alias = getAlias(AliasType.ROOM_NAME);
        if (alias != null) {
            return alias.getValue();
        }
        return null;
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
     * @return {@link Alias} for given {@code type}
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
     * Adds a new alias under which the room is accessible.
     *
     * @param type
     * @param value
     */
    public void addAlias(AliasType type, String value)
    {
        addAlias(new Alias(type, value));
    }

    /**
     * @return {@link #layout}
     */
    public RoomLayout getLayout()
    {
        return layout;
    }

    /**
     * @param layout sets the {@link #layout}
     */
    public void setLayout(RoomLayout layout)
    {
        this.layout = layout;
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
            existingRoomSetting.merge(roomSetting);
        }
        else {
            roomSettings.add(roomSetting);
        }
    }

    /**
     * @return {@link #participantRoles}
     */
    public List<RoomParticipantRole> getParticipantRoles()
    {
        return participantRoles;
    }

    /**
     * @param participantRoles sets the {@link #participantRoles}
     */
    public void setParticipantRoles(List<RoomParticipantRole> participantRoles)
    {
        this.participantRoles = participantRoles;
    }

    /**
     * @param userId to be added to the {@link #participantRoles}
     * @param role
     */
    public void addParticipantRole(String userId, ParticipantRole role)
    {
        for (RoomParticipantRole existingParticipant : participantRoles) {
            if (userId.equals(existingParticipant.getUserId())) {
                // Participant is already added to the room
                return;
            }
        }
        this.participantRoles.add(new RoomParticipantRole(userId, role));
    }

    /**
     * @param role
     * @return true whether {@link #participantRoles} contains {@link RoomParticipantRole} with given {@code role},
     *         false otherwise
     */
    public boolean hasParticipantWithRole(ParticipantRole role)
    {
        for (RoomParticipantRole participant : participantRoles) {
            if (role.equals(participant.getRole())) {
                return true;
            }
        }
        return false;
    }

    public static final String DESCRIPTION = "description";
    public static final String TECHNOLOGIES = "technologies";
    public static final String LICENSE_COUNT = "licenseCount";
    public static final String ALIASES = "aliases";
    public static final String LAYOUT = "layout";
    public static final String ROOM_SETTINGS = "roomSettings";
    public static final String PARTICIPANT_ROLES = "participantRoles";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(DESCRIPTION, description);
        dataMap.set(TECHNOLOGIES, technologies);
        dataMap.set(LICENSE_COUNT, licenseCount);
        dataMap.set(ALIASES, aliases);
        dataMap.set(LAYOUT, layout);
        dataMap.set(ROOM_SETTINGS, roomSettings);
        dataMap.set(PARTICIPANT_ROLES, participantRoles);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        description = dataMap.getString(DESCRIPTION);
        technologies = dataMap.getSet(TECHNOLOGIES, Technology.class);
        licenseCount = dataMap.getInt(LICENSE_COUNT);
        aliases = dataMap.getList(ALIASES, Alias.class);
        layout = dataMap.getEnum(LAYOUT, RoomLayout.class);
        roomSettings = dataMap.getList(ROOM_SETTINGS, RoomSetting.class);
        participantRoles = dataMap.getList(PARTICIPANT_ROLES, RoomParticipantRole.class);
    }

    @Override
    public String toString()
    {
        return String.format(Room.class.getSimpleName()
                + " (id: %s, description: %s, licenses: %d, layout: %s, aliases: %s, participantRoles: %s)",
                getId(), getDescription(), getLicenseCount(), getLayout(), getAliases(), getParticipantRoles());
    }
}
