package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.PersistentObject;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"parent_acl_record_id", "child_acl_record_id"}))
public class AclRecordDependency extends PersistentObject
{
    private String parentAclRecordId;

    private String childAclRecordId;

    @Column
    public String getParentAclRecordId()
    {
        return parentAclRecordId;
    }

    public void setParentAclRecordId(String parentAclRecordId)
    {
        this.parentAclRecordId = parentAclRecordId;
    }

    @Column
    public String getChildAclRecordId()
    {
        return childAclRecordId;
    }

    public void setChildAclRecordId(String childAclRecordId)
    {
        this.childAclRecordId = childAclRecordId;
    }


}
