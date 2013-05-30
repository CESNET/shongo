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
        ResourceRoomEndpoint sourceRoom = (ResourceRoomEndpoint) sourceExecutable;
        ResourceRoomEndpoint targetRoom = (ResourceRoomEndpoint) targetExecutable;
        return sourceRoom.getResource().equals(targetRoom.getResource());
    }

    /**
     * Perform migration.
     *
     * @param executor
     * @param executableManager
     */
    public void perform(Executor executor, ExecutableManager executableManager)
    {
        ResourceRoomEndpoint sourceRoom = (ResourceRoomEndpoint) executableManager.get(sourceExecutable.getId());
        ResourceRoomEndpoint targetRoom = (ResourceRoomEndpoint) executableManager.get(targetExecutable.getId());
        if (sourceRoom.getResource().equals(targetRoom.getResource())) {
            targetRoom.setState(Executable.State.STARTED);
            targetRoom.setRoomId(sourceRoom.getRoomId());
            targetRoom.update(executor, executableManager);
            if (targetRoom.getState().isStarted()) {
                sourceRoom.setState(Executable.State.STOPPED);
            }
        }
        else {
            // TODO: migrate room settings
        }
    }
}
