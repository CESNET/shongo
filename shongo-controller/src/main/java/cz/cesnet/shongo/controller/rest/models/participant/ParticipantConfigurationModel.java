package cz.cesnet.shongo.controller.rest.models.participant;

import cz.cesnet.shongo.ParticipantRole;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

/**
 * Configuration of {@link ParticipantModel}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Slf4j
public class ParticipantConfigurationModel
{

    /**
     * List of participants.
     */
    protected List<ParticipantModel> participants = new LinkedList<>();


    /**
     * @return {@link #participants}
     */
    public List<ParticipantModel> getParticipants()
    {
        return participants;
    }


    /**
     * @param participant to be added to the {@link #participants}
     */
    public void addParticipant(ParticipantModel participant)
    {
        if (participant.getType().equals(ParticipantModel.Type.USER)) {
            String userId = participant.getUserId();
            for (ParticipantModel existingParticipant : participants) {
                String existingUserId = existingParticipant.getUserId();
                ParticipantModel.Type existingType = existingParticipant.getType();
                if (existingType.equals(ParticipantModel.Type.USER) && existingUserId.equals(userId)) {
                    ParticipantRole existingRole = existingParticipant.getRole();
                    if (existingRole.compareTo(participant.getRole()) >= 0) {
                        log.warn("Skip adding {} because {} already exists.", participant, existingParticipant);
                        return;
                    }
                    else {
                        log.warn("Removing {} because {} will be added.", existingParticipant, participant);
                        participants.remove(existingParticipant);
                    }
                    break;
                }
            }
        }
        participants.add(participant);
    }

    /**
     * @param participant to be removed from the {@link #participants}
     */
    public void removeParticipant(ParticipantModel participant)
    {
        for (ParticipantModel existingParticipant : participants) {
            if (participant.getId().equals(existingParticipant.getId())) {
                participants.remove(existingParticipant);
                break;
            }
        }
    }

    /**
     * @param participant to be removed from the {@link #participants}
     */
    public ParticipantModel getParticipant(ParticipantModel participant)
    {
        for (ParticipantModel existingParticipant : participants) {
            if (participant.getId().equals(existingParticipant.getId())) {
                participants.remove(existingParticipant);
                return participant;
            }
        }
        return null;
    }
}
