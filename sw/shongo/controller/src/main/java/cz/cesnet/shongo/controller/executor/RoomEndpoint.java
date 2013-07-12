package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.controller.common.RoomConfiguration;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.Set;

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
    private RoomConfiguration roomConfiguration;

    /**
     * Description of the room which can be displayed to the user.
     */
    private String roomDescription;

    /**
     * @return {@link #roomConfiguration}
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
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
     * @return {@link #roomDescription}
     */
    @Column
    public String getRoomDescription()
    {
        return roomDescription;
    }

    /**
     * @param roomDescription sets the {@link #roomDescription}
     */
    public void setRoomDescription(String roomDescription)
    {
        this.roomDescription = roomDescription;
    }

    @Transient
    public final String getRoomDescriptionApi()
    {
        if (roomDescription != null) {
            return String.format("[exe:%d] %s", getId(), roomDescription);
        }
        else {
            return String.format("[exe:%d]", getId());
        }
    }

    @Override
    @Transient
    public int getCount()
    {
        return 0;
    }

    @Override
    @Transient
    public Set<Technology> getTechnologies()
    {
        if (roomConfiguration == null) {
            throw new IllegalStateException("Room configuration hasn't been set yet.");
        }
        return roomConfiguration.getTechnologies();
    }

    /**
     * @return {@link Technology} specific id of the {@link RoomConfiguration}.
     */
    @Transient
    public abstract String getRoomId();

    /**
     * @return {@link cz.cesnet.shongo.api.Room} representing the current room for the {@link RoomEndpoint}
     */
    @Transient
    public abstract Room getRoomApi();

    /**
     *
     * @param roomApi to be modified
     * @param executor to be used
     * @param executableManager
     * @throws ExecutorReportSet.RoomNotStartedException, ExecutorReportSet.CommandFailedException
     */
    public abstract void modifyRoom(Room roomApi, Executor executor,
            ExecutableManager executableManager)
            throws ExecutorReportSet.RoomNotStartedException, ExecutorReportSet.CommandFailedException;

    @Override
    protected void onCreate()
    {
        super.onCreate();

        onUpdate();
    }

    @PreUpdate
    protected void onUpdate()
    {
        if (roomConfiguration == null) {
            throw new IllegalStateException("Room configuration should not be null.");
        }
        if (roomConfiguration.getTechnologies().size() == 0) {
            throw new IllegalStateException("Room configuration should have some technologies.");
        }
    }
}
