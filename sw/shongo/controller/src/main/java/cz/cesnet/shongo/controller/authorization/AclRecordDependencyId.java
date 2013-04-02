package cz.cesnet.shongo.controller.authorization;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * Represents an identifier for {@link AclRecordDependency}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Embeddable
public class AclRecordDependencyId implements Serializable
{
    /**
     * Parent {@link AclRecord#id}.
     */
    private String parentAclRecordId;

    /**
     * Child {@link AclRecord#id}.
     */
    private String childAclRecordId;

    /**
     * Constructor.
     */
    public AclRecordDependencyId()
    {
    }

    /**
     * Constructor.
     *
     * @param parentAclRecordId sets the {@link #parentAclRecordId}
     * @param childAclRecordId  sets the {@link #childAclRecordId}
     */
    public AclRecordDependencyId(String parentAclRecordId, String childAclRecordId)
    {
        this.parentAclRecordId = parentAclRecordId;
        this.childAclRecordId = childAclRecordId;
    }

    /**
     * @return {@link #parentAclRecordId}
     */
    @Column(nullable = false)
    public String getParentAclRecordId()
    {
        return parentAclRecordId;
    }

    /**
     * @param parentAclRecordId sets the {@link #parentAclRecordId}
     */
    public void setParentAclRecordId(String parentAclRecordId)
    {
        this.parentAclRecordId = parentAclRecordId;
    }

    /**
     * @return {@link #childAclRecordId}
     */
    @Column(nullable = false)
    public String getChildAclRecordId()
    {
        return childAclRecordId;
    }

    /**
     * @param childAclRecordId sets the {@link #childAclRecordId}
     */
    public void setChildAclRecordId(String childAclRecordId)
    {
        this.childAclRecordId = childAclRecordId;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AclRecordDependencyId that = (AclRecordDependencyId) o;

        if (childAclRecordId != null ? !childAclRecordId
                .equals(that.childAclRecordId) : that.childAclRecordId != null) {
            return false;
        }
        if (parentAclRecordId != null ? !parentAclRecordId
                .equals(that.parentAclRecordId) : that.parentAclRecordId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = parentAclRecordId != null ? parentAclRecordId.hashCode() : 0;
        result = 31 * result + (childAclRecordId != null ? childAclRecordId.hashCode() : 0);
        return result;
    }
}
