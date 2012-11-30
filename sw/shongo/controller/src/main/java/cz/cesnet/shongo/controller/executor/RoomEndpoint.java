package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.common.Room;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Represents an {@link Endpoint} which represents a {@link Room} (is able to
 * interconnect multiple other {@link Endpoint}s).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class RoomEndpoint extends Endpoint
{
    /**
     * {@link cz.cesnet.shongo.Technology} specific identifier of the {@link Room}.
     */
    private String roomId;

    @Override
    @Transient
    public String getName()
    {
        return String.format("virtual room '%d'", getId());
    }

    @Override
    @Transient
    public int getCount()
    {
        return 0;
    }

    /**
     * @return {@link Room}
     */
    @Transient
    public abstract Room getRoom();

    /**
     * @return {@link #roomId}
     */
    @Column
    public String getRoomId()
    {
        return roomId;
    }

    /**
     * @param roomId sets the {@link #roomId}
     */
    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }
}
