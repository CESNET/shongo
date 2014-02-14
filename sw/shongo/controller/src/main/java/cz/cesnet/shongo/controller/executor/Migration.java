package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.recording.RecordableEndpoint;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.room.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.UsedRoomEndpoint;

import java.util.Map;

/**
 * Represents a migration from {@link #sourceExecutable} to {@link #targetExecutable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Migration
{
    /**
     * {@link cz.cesnet.shongo.controller.booking.executable.Executable} from which the migration should be performed.
     */
    private final Executable sourceExecutable;

    /**
     * {@link cz.cesnet.shongo.controller.booking.executable.Executable} to which the migration should be performed.
     */
    private final Executable targetExecutable;

    /**
     * Constructor.
     *
     * @param sourceExecutable sets the {@link #sourceExecutable}
     * @param targetExecutable sets the {@link #targetExecutable}
     */
    public Migration(Executable sourceExecutable, Executable targetExecutable)
    {
        if (!sourceExecutable.getSlotEnd().equals(targetExecutable.getSlotStart())) {
            throw new RuntimeException("Target executable doesn't start exactly when source executable ends.");
        }
        this.sourceExecutable = sourceExecutable;
        this.targetExecutable = targetExecutable;
    }

    /**
     * @return {@link #sourceExecutable}
     */
    public Executable getSourceExecutable()
    {
        return sourceExecutable;
    }

    /**
     * @return {@link #targetExecutable}
     */
    public Executable getTargetExecutable()
    {
        return targetExecutable;
    }

    /**
     * @return true whether the migration is replacement for starting/stopping actions of target/source executables
     *         (and thus the starting/stopping actions should not be performed),
     *         false otherwise
     */
    public boolean isReplacement()
    {
        RoomEndpoint sourceRoom = (RoomEndpoint) sourceExecutable;
        RoomEndpoint targetRoom = (RoomEndpoint) targetExecutable;

        if (sourceRoom instanceof ResourceRoomEndpoint && targetRoom instanceof ResourceRoomEndpoint) {
            ResourceRoomEndpoint sourceResourceRoom = (ResourceRoomEndpoint) sourceRoom;
            ResourceRoomEndpoint targetResourceRoom = (ResourceRoomEndpoint) targetRoom;
            if (sourceResourceRoom.getResource().equals(targetResourceRoom.getResource())) {
                return true;
            }
        }
        if (sourceRoom instanceof UsedRoomEndpoint && targetRoom instanceof UsedRoomEndpoint) {
            UsedRoomEndpoint sourceUsedRoom = (UsedRoomEndpoint) sourceRoom;
            UsedRoomEndpoint targetUsedRoom = (UsedRoomEndpoint) targetRoom;
            if (sourceUsedRoom.getReusedRoomEndpoint().equals(targetUsedRoom.getReusedRoomEndpoint())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Perform migration.
     *
     * @param executor
     * @param executableManager
     */
    public boolean perform(Executor executor, ExecutableManager executableManager)
    {
        RoomEndpoint sourceRoom = (RoomEndpoint) executableManager.get(sourceExecutable.getId());
        RoomEndpoint targetRoom = (RoomEndpoint) executableManager.get(targetExecutable.getId());

        // Migrate between resource rooms
        if (sourceRoom instanceof ResourceRoomEndpoint && targetRoom instanceof ResourceRoomEndpoint) {
            ResourceRoomEndpoint sourceResourceRoom = (ResourceRoomEndpoint) sourceRoom;
            ResourceRoomEndpoint targetResourceRoom = (ResourceRoomEndpoint) targetRoom;

            // Reuse the same room in the device
            if (sourceResourceRoom.getResource().equals(targetResourceRoom.getResource())) {
                targetResourceRoom.setState(Executable.State.STARTED);
                targetResourceRoom.setModified(true);
                targetResourceRoom.setRoomId(sourceRoom.getRoomId());
                Boolean result = targetResourceRoom.update(executor, executableManager);
                if (Boolean.TRUE.equals(result)) {
                    targetResourceRoom.setState(Executable.State.STARTED);
                    sourceResourceRoom.setState(Executable.State.STOPPED);
                    sourceResourceRoom.setModified(false);
                    return true;
                }
                else {
                    targetResourceRoom.setState(Executable.State.STARTING_FAILED);
                }
            }
        }
        // Migrate between used rooms
        else if (sourceRoom instanceof UsedRoomEndpoint && targetRoom instanceof UsedRoomEndpoint) {
            UsedRoomEndpoint sourceUsedRoom = (UsedRoomEndpoint) sourceRoom;
            UsedRoomEndpoint targetUsedRoom = (UsedRoomEndpoint) targetRoom;

            // If the same room is reused, only update the room
            if (sourceUsedRoom.getReusedRoomEndpoint().equals(targetUsedRoom.getReusedRoomEndpoint())) {
                targetUsedRoom.setState(Executable.State.STARTED);
                targetUsedRoom.setModified(true);
                Boolean result = targetUsedRoom.update(executor, executableManager);
                if (Boolean.TRUE.equals(result)) {
                    targetUsedRoom.setState(Executable.State.STARTED);
                    sourceUsedRoom.setState(Executable.State.STOPPED);
                    sourceUsedRoom.setModified(false);
                    return true;
                }
                else {
                    targetUsedRoom.setState(Executable.State.STARTING_FAILED);
                }
            }
        }
        return false;
    }
}
