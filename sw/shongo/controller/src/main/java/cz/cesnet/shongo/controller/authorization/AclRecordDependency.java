package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.SimplePersistentObject;

import javax.persistence.*;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"parent_acl_record_id", "child_acl_record_id"}))
public class AclRecordDependency extends SimplePersistentObject
{
    /**
     * Parent {@link AclRecord}.
     */
    private AclRecord parentAclRecord;

    /**
     * Parent {@link AclRecord}.
     */
    private AclRecord childAclRecord;

    /**
     * @see Type
     */
    private Type type;

    /**
     * @return {@link #parentAclRecord}
     */
    @ManyToOne
    @JoinColumn(name = "parent_acl_record_id")
    public AclRecord getParentAclRecord()
    {
        return parentAclRecord;
    }

    /**
     * @param parentAclRecord sets the {@link #parentAclRecord}
     */
    public void setParentAclRecord(AclRecord parentAclRecord)
    {
        this.parentAclRecord = parentAclRecord;
    }

    /**
     * @return {@link #childAclRecord}
     */
    @ManyToOne
    @JoinColumn(name = "child_acl_record_id")
    public AclRecord getChildAclRecord()
    {
        return childAclRecord;
    }

    /**
     * @param childAclRecord sets the {@link #childAclRecord}
     */
    public void setChildAclRecord(AclRecord childAclRecord)
    {
        this.childAclRecord = childAclRecord;
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
         * TODO:
         */
        DELETE_CASCADE,

        /**
         * TODO:
         */
        DELETE_DETACH
    }
}
