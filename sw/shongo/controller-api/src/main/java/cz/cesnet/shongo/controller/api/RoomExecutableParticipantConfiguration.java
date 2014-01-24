package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents a participant configuration for a {@link AbstractRoomExecutable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomExecutableParticipantConfiguration extends ExecutableConfiguration
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
     * @param participantId
     * @return {@link AbstractParticipant} with given {@code participantId} or null
     */
    public AbstractParticipant getParticipant(String participantId)
    {
        for (AbstractParticipant participant : participants) {
            if (participantId.equals(participant.getId())) {
                return participant;
            }
        }
        return null;
    }

    /**
     * @param participant to be added to the {@link #participants}
     */
    public void addParticipant(AbstractParticipant participant)
    {
        participants.add(participant);
    }

    /**
     * Clear {@link #participants}.
     */
    public void clearParticipants()
    {
        participants.clear();
    }

    /**
     * @param participant to be removed from the {@link #participants}
     */
    public void removeParticipant(AbstractParticipant participant)
    {
        participants.remove(participant);
    }

    /**
     * @param participantId to be removed from the {@link #participants}
     */
    public void removeParticipantById(String participantId)
    {
        for (AbstractParticipant participant : participants) {
            if (participantId.equals(participant.getId())) {
                participants.remove(participant);
                break;
            }
        }
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
