package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.RoomSetting;

import java.util.LinkedList;
import java.util.List;

/**
 * {@link Specification} for a room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomSpecification extends Specification
{
    /**
     * If {@link RoomEstablishment} is set a new room shall be created, else existing room shall be reused.
     */
    private RoomEstablishment establishment;

    /**
     * If {@link RoomAvailability} is set the room shall be available for joining, else it shall be inaccessible.
     */
    private RoomAvailability availability;

    /**
     * {@link cz.cesnet.shongo.api.RoomSetting}s for the virtual room.
     */
    private List<RoomSetting> roomSettings = new LinkedList<RoomSetting>();

    /**
     * Collection of {@link AbstractParticipant}s for the virtual room.
     */
    private List<AbstractParticipant> participants = new LinkedList<AbstractParticipant>();

    /**
     * Constructor.
     */
    public RoomSpecification()
    {
    }

    /**
     * Constructor for ad-hoc room.
     *
     * @param participantCount sets the {@link RoomAvailability#participantCount}
     * @param technology       to be added to the {@link RoomEstablishment#technologies}
     */
    public RoomSpecification(int participantCount, Technology technology)
    {
        setEstablishment(new RoomEstablishment(technology));
        setAvailability(new RoomAvailability(participantCount));
    }

    /**
     * Constructor for ad-hoc room.
     *
     * @param participantCount sets the {@link RoomAvailability#participantCount}
     * @param technology       to be added to the {@link RoomEstablishment#technologies}
     * @param resourceId       sets the {@link RoomEstablishment#resourceId}
     */
    public RoomSpecification(int participantCount, Technology technology, String resourceId)
    {
        setEstablishment(new RoomEstablishment(technology, resourceId));
        setAvailability(new RoomAvailability(participantCount));
    }

    /**
     * Constructor for ad-hoc room.
     *
     * @param participantCount sets the {@link RoomAvailability#participantCount}
     * @param technologies     sets the {@link RoomEstablishment#technologies}
     */
    public RoomSpecification(int participantCount, Technology[] technologies)
    {
        setEstablishment(new RoomEstablishment(technologies));
        setAvailability(new RoomAvailability(participantCount));
    }

    /**
     * Constructor for permanent room.
     *
     * @param technology to be added to the {@link RoomEstablishment#technologies}
     */
    public RoomSpecification(Technology technology)
    {
        setEstablishment(new RoomEstablishment(technology));
    }

    /**
     * Constructor for permanent room.
     *
     * @param technology to be added to the {@link RoomEstablishment#technologies}
     * @param resourceId sets the {@link RoomEstablishment#resourceId}
     */
    public RoomSpecification(Technology technology, String resourceId)
    {
        setEstablishment(new RoomEstablishment(technology, resourceId));
    }

    /**
     * Constructor for permanent room.
     *
     * @param technologies sets the {@link RoomEstablishment#technologies}
     */
    public RoomSpecification(Technology[] technologies)
    {
        setEstablishment(new RoomEstablishment(technologies));
    }

    /**
     * Constructor for permanent room.
     *
     * @param aliasType to be added to the {@link RoomEstablishment#aliasSpecifications}
     */
    public RoomSpecification(AliasType aliasType)
    {
        setEstablishment(new RoomEstablishment(aliasType));
    }

    /**
     * Constructor for permanent room.
     *
     * @param aliasTypes sets the {@link RoomEstablishment#aliasSpecifications}
     */
    public RoomSpecification(AliasType[] aliasTypes)
    {
        setEstablishment(new RoomEstablishment(aliasTypes));
    }

    /**
     * Constructor for reused room.
     *
     * @param participantCount sets the {@link RoomAvailability#participantCount}
     */
    public RoomSpecification(int participantCount)
    {
        setAvailability(new RoomAvailability(participantCount));
    }

    /**
     * @return {@link #establishment}
     */
    public RoomEstablishment getEstablishment()
    {
        return establishment;
    }

    /**
     * @param establishment sets the {@link #establishment}
     */
    public void setEstablishment(RoomEstablishment establishment)
    {
        this.establishment = establishment;
    }

    /**
     * @return newly created {@link #establishment}
     */
    public RoomEstablishment createEstablishment()
    {
        this.establishment = new RoomEstablishment();
        return this.establishment;
    }

    /**
     * @return {@link #availability}
     */
    public RoomAvailability getAvailability()
    {
        return availability;
    }

    /**
     * @param availability sets the {@link #availability}
     */
    public void setAvailability(RoomAvailability availability)
    {
        this.availability = availability;
    }

    /**
     * @return newly created {@link #availability}
     */
    public RoomAvailability createAvailability()
    {
        this.availability = new RoomAvailability();
        return this.availability;
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
     * @param roomSetting to be added to the {@link #roomSettings}
     */
    public void addRoomSetting(RoomSetting roomSetting)
    {
        roomSettings.add(roomSetting);
    }

    /**
     * @param roomSetting to be removed from the {@link #roomSettings}
     */
    public void removeRoomSetting(RoomSetting roomSetting)
    {
        roomSettings.remove(roomSetting);
    }

    /**
     * @return {@link #participants}
     */
    public List<AbstractParticipant> getParticipants()
    {
        return participants;
    }

    /**
     * @param participant to be added to the {@link #participants}
     */
    public void addParticipant(AbstractParticipant participant)
    {
        participants.add(participant);
    }

    public static final String ESTABLISHMENT = "establishment";
    public static final String AVAILABILITY = "availability";
    public static final String ROOM_SETTINGS = "roomSettings";
    public static final String PARTICIPANTS = "participants";


    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ESTABLISHMENT, establishment);
        dataMap.set(AVAILABILITY, availability);
        dataMap.set(ROOM_SETTINGS, roomSettings);
        dataMap.set(PARTICIPANTS, participants);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        establishment = dataMap.getComplexType(ESTABLISHMENT, RoomEstablishment.class);
        availability = dataMap.getComplexType(AVAILABILITY, RoomAvailability.class);
        roomSettings = dataMap.getList(ROOM_SETTINGS, RoomSetting.class);
        participants = dataMap.getList(PARTICIPANTS, AbstractParticipant.class);
    }

}
