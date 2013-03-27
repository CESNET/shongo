package cz.cesnet.shongo.controller.authorization;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AclRecordParent
{
    /**
     *
     */
    private String childAclRecordId;

    private String parentAclRecordId;

    public String getChildAclRecordId()
    {
        return childAclRecordId;
    }

    public void setChildAclRecordId(String childAclRecordId)
    {
        this.childAclRecordId = childAclRecordId;
    }

    public String getParentAclRecordId()
    {
        return parentAclRecordId;
    }

    public void setParentAclRecordId(String parentAclRecordId)
    {
        this.parentAclRecordId = parentAclRecordId;
    }
}
