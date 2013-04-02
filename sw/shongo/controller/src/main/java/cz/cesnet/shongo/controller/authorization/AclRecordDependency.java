package cz.cesnet.shongo.controller.authorization;

import javax.persistence.*;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AclRecordDependency
{
    private AclRecordDependencyId id;

    public AclRecordDependency()
    {
    }

    public AclRecordDependency(String parentAclRecordId, String childAclRecordId)
    {
        this.id = new AclRecordDependencyId(parentAclRecordId, childAclRecordId);
    }

    @EmbeddedId
    public AclRecordDependencyId getId()
    {
        return id;
    }

    public void setId(AclRecordDependencyId id)
    {
        this.id = id;
    }

    @Transient
    public String getParentAclRecordId()
    {
        return id.getParentAclRecordId();
    }

    @Transient
    public String getChildAclRecordId()
    {
        return id.getChildAclRecordId();
    }

}
