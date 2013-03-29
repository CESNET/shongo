package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.PersistentObject;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AclRecordDeleteRequest extends PersistentObject
{
    private String aclRecordId;

    @Column
    public String getAclRecordId()
    {
        return aclRecordId;
    }

    public void setAclRecordId(String aclRecordId)
    {
        this.aclRecordId = aclRecordId;
    }
}
