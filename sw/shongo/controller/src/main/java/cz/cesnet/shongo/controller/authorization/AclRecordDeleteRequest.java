package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.PersistentObject;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Represents a request for deletion of {@link AclRecord}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AclRecordDeleteRequest extends PersistentObject
{
    /**
     * {@link AclRecord#id}
     */
    private String aclRecordId;

    /**
     * @return {@link #aclRecordId}
     */
    @Column
    public String getAclRecordId()
    {
        return aclRecordId;
    }

    /**
     * @param aclRecordId sets the {@link #aclRecordId}
     */
    public void setAclRecordId(String aclRecordId)
    {
        this.aclRecordId = aclRecordId;
    }
}
