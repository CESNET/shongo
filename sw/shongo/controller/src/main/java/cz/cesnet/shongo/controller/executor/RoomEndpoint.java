package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.common.RoomConfiguration;

import javax.persistence.*;

/**
 * Represents an {@link Endpoint} which represents a {@link RoomConfiguration} (is able to
 * interconnect multiple other {@link Endpoint}s).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class RoomEndpoint extends Endpoint
{
    /**
     * @see RoomConfiguration
     */
    private RoomConfiguration roomConfiguration = new RoomConfiguration();

    /**
     * Name of the room which can be displayed to the user.
     */
    private String roomName;

    /**
     * @return {@link #roomConfiguration}
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    public RoomConfiguration getRoomConfiguration()
    {
        return roomConfiguration;
    }

    /**
     * @param roomConfiguration sets the {@link #roomConfiguration}
     */
    public void setRoomConfiguration(RoomConfiguration roomConfiguration)
    {
        this.roomConfiguration = roomConfiguration;
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
     * @return {@link Technology} specific id of the {@link RoomConfiguration}.
     */
    @Transient
    public abstract String getRoomId();
}
