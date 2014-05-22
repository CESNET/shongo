package cz.cesnet.shongo.controller.acl;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.controller.AclIdentityType;
import cz.cesnet.shongo.controller.api.Controller;

import javax.persistence.*;

/**
 * Represents an identity (e.g., User or Group) for which can be created {@link AclEntry}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"type", "principal_id"}))
@NamedQuery(name = "AclIdentity.find", query = "SELECT i FROM AclIdentity i WHERE i.type = :type AND i.principalId = :principalId")
public class AclIdentity extends SimplePersistentObject
{
    /**
     * Type of identity.
     *
     * @see cz.cesnet.shongo.controller.AclIdentityType
     */
    private AclIdentityType type;

    /**
     * Unique principal identifier for the {@link #type}.
     */
    private String principalId;

    /**
     * @return {@link #type}
     */
    @Column(name = "type", nullable = false, length = AbstractComplexType.ENUM_COLUMN_LENGTH)
    @Enumerated(EnumType.STRING)
    public AclIdentityType getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(AclIdentityType type)
    {
        this.type = type;
    }

    /**
     * @return {@link #principalId}
     */
    @Column(name = "principal_id", nullable = false, length = Controller.USER_ID_COLUMN_LENGTH)
    public String getPrincipalId()
    {
        return principalId;
    }

    /**
     * @param principal sets the {@link #principalId}
     */
    public void setPrincipalId(String principal)
    {
        this.principalId = principal;
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
        AclIdentity that = (AclIdentity) o;
        if (!principalId.equals(that.principalId)) {
            return false;
        }
        if (type != that.type) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = type.hashCode();
        result = 31 * result + principalId.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return type + ":" + principalId;
    }
}
