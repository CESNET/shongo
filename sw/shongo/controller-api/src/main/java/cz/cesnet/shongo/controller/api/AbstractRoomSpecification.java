package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.RoomSetting;

import java.util.LinkedList;
import java.util.List;

/**
 * {@link cz.cesnet.shongo.controller.api.Specification} for a meeting room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractRoomSpecification extends Specification
{
    /**
     * {@link RoomSetting}s for the virtual room.
     */
    private List<RoomSetting> roomSettings = new LinkedList<RoomSetting>();

    /**
     * Collection of {@link AbstractParticipant}s for the virtual room.
     */
    private List<AbstractParticipant> participants = new LinkedList<AbstractParticipant>();

    /**
     * {@link ExecutableServiceSpecification}s for the virtual room.
     */
    private List<ExecutableServiceSpecification> serviceSpecifications = new LinkedList<ExecutableServiceSpecification>();

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

    /**
     * @return {@link #serviceSpecifications}
     */
    public List<ExecutableServiceSpecification> getServiceSpecifications()
    {
        return serviceSpecifications;
    }

    /**
     * @param serviceSpecification to be added to the {@link #serviceSpecifications}
     */
    public void addServiceSpecification(ExecutableServiceSpecification serviceSpecification)
    {
        serviceSpecifications.add(serviceSpecification);
    }

    public static final String ROOM_SETTINGS = "roomSettings";
    public static final String PARTICIPANTS = "participants";
    public static final String SERVICE_SPECIFICATIONS = "serviceSpecifications";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ROOM_SETTINGS, roomSettings);
        dataMap.set(PARTICIPANTS, participants);
        dataMap.set(SERVICE_SPECIFICATIONS, serviceSpecifications);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        roomSettings = dataMap.getList(ROOM_SETTINGS, RoomSetting.class);
        participants = dataMap.getList(PARTICIPANTS, AbstractParticipant.class);
        serviceSpecifications = dataMap.getList(SERVICE_SPECIFICATIONS, ExecutableServiceSpecification.class);
    }
}
