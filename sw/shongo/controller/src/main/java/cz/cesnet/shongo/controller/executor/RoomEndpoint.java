package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.common.RoomConfiguration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Represents an {@link Endpoint} which represents a {@link cz.cesnet.shongo.controller.common.RoomConfiguration} (is able to
 * interconnect multiple other {@link Endpoint}s).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class RoomEndpoint extends Endpoint
{
    /**
     * {@link cz.cesnet.shongo.Technology} specific id of the {@link cz.cesnet.shongo.controller.common.RoomConfiguration}.
     */
    private String roomId;

    /**
     * Name of the room which can be displayed to the user.
     */
    private String roomName;

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
     * @return {@link cz.cesnet.shongo.controller.common.RoomConfiguration}
     */
    @Transient
    public abstract RoomConfiguration getRoomConfiguration();

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

    /**
     * @return {@link #roomName}
     */
    public String getRoomName()
    {
        return roomName;
    }

    /**
     * @param roomName sets the {@link #roomName}
     */
    public void setRoomName(String roomName)
    {
        this.roomName = roomName;
    }
}
