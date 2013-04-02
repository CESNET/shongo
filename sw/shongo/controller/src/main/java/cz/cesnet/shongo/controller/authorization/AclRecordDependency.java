package cz.cesnet.shongo.controller.authorization;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Represents a dependency from child {@link AclRecord} to a parent {@link AclRecord}. Child {@link AclRecord} is
 * automatically created for the parent {@link AclRecord} and child {@link AclRecord} can be deleted only when all
 * parent {@link AclRecord}s has been deleted.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AclRecordDependency
{
    /**
     * @see AclRecordDependencyId
     */
    private AclRecordDependencyId id;

    /**
     * @return {@link #id}
     */
    @EmbeddedId
    public AclRecordDependencyId getId()
    {
        return id;
    }

    /**
     * @param id sets the {@link #id}
     */
    public void setId(AclRecordDependencyId id)
    {
        this.id = id;
    }

    /**
     * @return {@link #id#getParentAclRecordId()}
     */
    @Transient
    public String getParentAclRecordId()
    {
        return id.getParentAclRecordId();
    }

    /**
     * @return {@link #id#getChildAclRecordId()}
     */
    @Transient
    public String getChildAclRecordId()
    {
        return id.getChildAclRecordId();
    }

}
