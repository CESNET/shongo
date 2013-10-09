package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.controller.api.AbstractParticipant;

import java.util.List;

/**
 * Represents a model which have list of {@link AbstractParticipant}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ParticipantContainer
{
    /**
     * @return list of {@link AbstractParticipant}
     */
    public List<ParticipantModel> getParticipants();

    /**
     * @param participant to be added
     */
    public void addParticipant(ParticipantModel participant);

    /**
     * @param participant to be removed
     */
    public void removeParticipant(ParticipantModel participant);
}
