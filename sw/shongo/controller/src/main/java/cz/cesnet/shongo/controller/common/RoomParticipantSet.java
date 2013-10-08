package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.PersistentObject;

import javax.persistence.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a set of {@link AbstractParticipant}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RoomParticipantSet extends PersistentObject
{
    /**
     * List of {@link AbstractParticipant}s.
     */
    private List<AbstractParticipant> participants = new LinkedList<AbstractParticipant>();

    /**
     * @return {@link #participants}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<AbstractParticipant> getParticipants()
    {
        return participants;
    }
}
