package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.Executor;

import javax.persistence.*;

/**
 * Represents a migration from {@link #sourceExecutable} to {@link #targetExecutable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class Migration extends PersistentObject
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
    @OneToOne(optional = false)
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
            if (sourceUsedRoom.getRoomEndpoint().equals(targetUsedRoom.getRoomEndpoint())) {
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
    public void perform(Executor executor, ExecutableManager executableManager)
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
                targetResourceRoom.setRoomId(sourceRoom.getRoomId());
                targetResourceRoom.update(executor, executableManager);
                if (targetResourceRoom.getState().isStarted()) {
                    sourceResourceRoom.setState(Executable.State.STOPPED);
                }
            }
            // Migrate room between devices
            else {
                // TODO: migrate room settings
            }
        }
        // Migrate between used rooms
        else if (sourceRoom instanceof UsedRoomEndpoint && targetRoom instanceof UsedRoomEndpoint) {
            UsedRoomEndpoint sourceUsedRoom = (UsedRoomEndpoint) sourceRoom;
            UsedRoomEndpoint targetUsedRoom = (UsedRoomEndpoint) targetRoom;

            // If the same room is reused, only update the room
            if (sourceUsedRoom.getRoomEndpoint().equals(targetUsedRoom.getRoomEndpoint())) {
                targetUsedRoom.setState(Executable.State.STARTED);
                targetUsedRoom.update(executor, executableManager);
                if (targetUsedRoom.getState().isStarted()) {
                    sourceUsedRoom.setState(Executable.State.STOPPED);
                }
            }
        }
    }
}
