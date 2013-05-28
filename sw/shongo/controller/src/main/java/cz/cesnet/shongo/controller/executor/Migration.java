package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.PersistentObject;

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

    @PostLoad
    @PrePersist
    protected void validate()
    {
        if (!this.sourceExecutable.getSlotEnd().equals(this.targetExecutable.getSlotStart())) {
            throw new RuntimeException("Target executable doesn't start exactly when source executable ends.");
        }
    }
}
