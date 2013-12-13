package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.controller.acl.AclEntry;

import javax.persistence.*;

/**
 * Represents a parent-child dependency between two {@link cz.cesnet.shongo.controller.acl.AclEntry}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"parent_acl_entry_id", "child_acl_entry_id"}))
public class AclEntryDependency extends SimplePersistentObject
{
    /**
     * Parent {@link cz.cesnet.shongo.controller.acl.AclEntry} which owns the {@link #childAclEntry}.
     */
    private AclEntry parentAclEntry;

    /**
     * Child {@link AclEntry} which is owned by {@link #parentAclEntry}.
     */
    private AclEntry childAclEntry;

    /**
     * @see Type
     */
    private Type type;

    /**
     * @return {@link #parentAclEntry}
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "parent_acl_entry_id")
    public AclEntry getParentAclEntry()
    {
        return parentAclEntry;
    }

    /**
     * @param parentAclRecord sets the {@link #parentAclEntry}
     */
    public void setParentAclEntry(AclEntry parentAclRecord)
    {
        this.parentAclEntry = parentAclRecord;
    }

    /**
     * @return {@link #childAclEntry}
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "child_acl_entry_id")
    public AclEntry getChildAclEntry()
    {
        return childAclEntry;
    }

    /**
     * @param childAclRecord sets the {@link #childAclEntry}
     */
    public void setChildAclEntry(AclEntry childAclRecord)
    {
        this.childAclEntry = childAclRecord;
    }

    /**
     * @return {@link #type}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public Type getType()
    {
        return type;
    }

    /**
     * @param type {@link #type}
     */
    public void setType(Type type)
    {
        this.type = type;
    }

    /**
     * Type of dependency.
     */
    public static enum Type
    {
        /**
         * When parent entry is deleted, the child entry should be also deleted.
         */
        DELETE_CASCADE,

        /**
         * When parent entry is deleted, the child entry should not be deleted.
         */
        DELETE_DETACH
    }
}
