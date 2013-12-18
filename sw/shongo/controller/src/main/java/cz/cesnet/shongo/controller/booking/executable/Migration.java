package cz.cesnet.shongo.controller.booking.executable;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.controller.booking.recording.RecordableEndpoint;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.booking.room.ResourceRoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.UsedRoomEndpoint;

import javax.persistence.*;
import java.util.Map;

/**
 * Represents a migration from {@link #sourceExecutable} to {@link #targetExecutable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class Migration extends SimplePersistentObject
{
    /**
     * {@link Executable} from which the migration should be performed.
     */
    private Executable sourceExecutable;

    /**
     * {@link Executable} to which the migration should be performed.
     */
    private Executable targetExecutable;

    /**
     * @return {@link #sourceExecutable}
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "source_executable_id")
    @Access(AccessType.FIELD)
    public Executable getSourceExecutable()
    {
        return sourceExecutable;
    }

    /**
     * @param sourceExecutable sets the {@link #sourceExecutable}
     */
    public void setSourceExecutable(Executable sourceExecutable)
    {
        this.sourceExecutable = sourceExecutable;
    }

    /**
     * @return {@link #targetExecutable}
     */
    @OneToOne(optional = false)
    @JoinColumn(name = "target_executable_id")
    @Access(AccessType.FIELD)
    public Executable getTargetExecutable()
    {
        return targetExecutable;
    }

    /**
     * @param targetExecutable sets the {@link #targetExecutable}
     */
    public void setTargetExecutable(Executable targetExecutable)
    {
        // Manage bidirectional association
        if (targetExecutable != this.targetExecutable) {
            if (this.targetExecutable != null) {
                Executable oldTargetExecutable = this.targetExecutable;
                this.targetExecutable = null;
                oldTargetExecutable.setMigration(null);
            }
            if (targetExecutable != null) {
                this.targetExecutable = targetExecutable;
                this.targetExecutable.setMigration(this);
            }
        }
    }

    @PrePersist
    protected void validate()
    {
        if (!this.sourceExecutable.getSlotEnd().equals(this.targetExecutable.getSlotStart())) {
            throw new RuntimeException("Target executable doesn't start exactly when source executable ends.");
        }
    }

    /**
     * @return true whether the migration is replacement for starting/stopping actions of target/source executables
     *         (and thus the starting/stopping actions should not be performed),
     *         false otherwise
     */
    @Transient
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
                    migrateRecordingFolders(sourceResourceRoom, targetResourceRoom);
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
                    migrateRecordingFolders(sourceUsedRoom, targetUsedRoom);
                    return true;
                }
                else {
                    targetUsedRoom.setState(Executable.State.STARTING_FAILED);
                }
            }
        }
        return false;
    }

    /**
     * Migrate {@link RecordableEndpoint#getRecordingFolderIds()} from {@code sourceRoom} to {@code targetRoom}.
     *
     * @param sourceRoom
     * @param targetRoom
     */
    private void migrateRecordingFolders(RecordableEndpoint sourceRoom, RecordableEndpoint targetRoom)
    {
        for(Map.Entry<RecordingCapability, String> entry : sourceRoom.getRecordingFolderIds().entrySet()) {
            targetRoom.putRecordingFolderId(entry.getKey(), entry.getValue());
        }
    }
}
