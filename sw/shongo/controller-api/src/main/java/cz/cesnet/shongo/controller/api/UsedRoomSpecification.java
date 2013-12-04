package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;

/**
 * {@link cz.cesnet.shongo.controller.api.Specification} for a one time room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UsedRoomSpecification extends AbstractRoomSpecification
{
    /**
     * Shongo-id for {@link RoomExecutable}.
     */
    private String roomExecutableId;

    /**
     * Number of ports which must be allocated for the virtual room.
     */
    private Integer participantCount;

    /**
     * Constructor.
     */
    public UsedRoomSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param roomExecutableId sets the {@link #roomExecutableId}
     * @param participantCount sets the {@link #participantCount}
     */
    public UsedRoomSpecification(String roomExecutableId, int participantCount)
    {
        setRoomExecutableId(roomExecutableId);
        setParticipantCount(participantCount);
    }

    /**
     * @return {@link #roomExecutableId}
     */
    public String getRoomExecutableId()
    {
        return roomExecutableId;
    }

    /**
     * @param roomExecutableId sets the {@link #roomExecutableId}
     */
    public void setRoomExecutableId(String roomExecutableId)
    {
        this.roomExecutableId = roomExecutableId;
    }

    /**
     * @return {@link #participantCount}
     */
    public Integer getParticipantCount()
    {
        return participantCount;
    }

    /**
     * @param participantCount sets the {@link #participantCount}
     */
    public void setParticipantCount(Integer participantCount)
    {
        this.participantCount = participantCount;
    }

    public static final String ROOM_EXECUTABLE_ID = "roomExecutableId";
    public static final String PARTICIPANT_COUNT = "participantCount";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ROOM_EXECUTABLE_ID, roomExecutableId);
        dataMap.set(PARTICIPANT_COUNT, participantCount);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        roomExecutableId = dataMap.getString(ROOM_EXECUTABLE_ID);
        participantCount = dataMap.getIntegerRequired(PARTICIPANT_COUNT);
    }
}
