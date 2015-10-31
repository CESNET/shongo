package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.controller.api.RoomExecutable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.executor.ExecutionReportSet;
import cz.cesnet.shongo.controller.executor.Executor;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Represents a TODO  which acts as {@link RoomEndpoint}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@Entity
public class ForeignRoomEndpoint extends RoomEndpoint
{
    @Transient
    @Override
    public DeviceResource getResource()
    {
        return null;
    }

    @Transient
    @Override
    public String getRoomId()
    {
        return null;
    }

    @Transient
    @Override
    public void modifyRoom(Room roomApi, Executor executor) throws ExecutionReportSet.RoomNotStartedException, ExecutionReportSet.CommandFailedException
    {
        throw new TodoImplementException();
    }

    @Override
    protected State onStart(Executor executor, ExecutableManager executableManager)
    {
        return State.STARTED;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Executable createApi()
    {
        return new RoomExecutable();
    }
}
