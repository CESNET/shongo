package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.DataMap;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents a participant configuration for a {@link AbstractRoomExecutable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomExecutableParticipantConfiguration extends AbstractComplexType
{
    /**
     * Collection of {@link AbstractParticipant}s for the room.
     */
    private List<AbstractParticipant> participants = new LinkedList<AbstractParticipant>();

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

    public static final String PARTICIPANTS = "participants";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(PARTICIPANTS, participants);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        participants = dataMap.getList(PARTICIPANTS, AbstractParticipant.class);
    }
}
