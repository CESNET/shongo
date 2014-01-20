package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import org.joda.time.Interval;

import javax.persistence.*;

/**
 * Represents a {@link Notification} for a {@link AbstractParticipant}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ParticipantNotification extends Notification
{
    /**
     * {@link AbstractParticipant} for which the {@link ParticipantNotification} is created.
     */
    private AbstractParticipant participant;

    /**
     * @see Type
     */
    private Type type;

    /**
     * @return {@link #participant}
     */
    @ManyToOne
    public AbstractParticipant getParticipant()
    {
        return participant;
    }

    /**
     * @param participant sets the {@link #participant}
     */
    public void setParticipant(AbstractParticipant participant)
    {
        this.participant = participant;
    }

    /**
     * @return {@link #type}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public Type getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(Type type)
    {
        this.type = type;
    }

    /**
     * Available types of {@link ParticipantNotification}s.
     */
    public static enum Type
    {
        /**
         * {@link ParticipantNotification} that a {@link RoomEndpoint} in which {@link AbstractParticipant}
         * is configured will start soon.
         */
        ROOM_START,

        /**
         * {@link ParticipantNotification} that a the {@link AbstractParticipant} has been configured to
         * a {@link RoomEndpoint}.
         */
        ROOM_PARTICIPATION,

        /**
         * {@link ParticipantNotification} that a {@link RoomEndpoint} in which {@link AbstractParticipant}
         * is configured was modified.
         */
        ROOM_MODIFIED,

        /**
         * {@link ParticipantNotification} that a {@link RoomEndpoint} in which {@link AbstractParticipant}
         * is configured was deleted.
         */
        ROOM_DELETED,
    }
}
