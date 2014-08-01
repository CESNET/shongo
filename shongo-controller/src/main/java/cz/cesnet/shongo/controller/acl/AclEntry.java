package cz.cesnet.shongo.controller.acl;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.authorization.Authorization;

import javax.persistence.*;

/**
 * Represents an ACL entry for {@link #identity} to be granted by {@link #role} for the {@link #objectIdentity}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"acl_identity_id", "acl_object_identity_id", "role"}))
@NamedQueries({
        @NamedQuery(name="AclEntry.find", query="SELECT e FROM AclEntry e WHERE e.identity = :identity AND e.objectIdentity = :objectIdentity AND e.role = :role"),
        @NamedQuery(name="AclEntry.findByIdentity", query="SELECT e FROM AclEntry e WHERE e.identity IN(:identities)"),
        @NamedQuery(name="AclEntry.findByObjectIdentity", query="SELECT e FROM AclEntry e WHERE e.objectIdentity = :objectIdentity"),
        @NamedQuery(name="AclEntry.findByObjectIdentityAndRole", query="SELECT e FROM AclEntry e WHERE e.objectIdentity = :objectIdentity AND e.role = :role")
})
public class AclEntry extends SimplePersistentObject
{
    /**
     * @see AclIdentity
     */
    private AclIdentity identity;

    /**
     * @see AclObjectIdentity
     */
    private AclObjectIdentity objectIdentity;

    /**
     * Role which is granted to {@link #identity} for {@link #objectIdentity}.
     */
    private String role;

    /**
     * @return {@link #identity}
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "acl_identity_id")
    public AclIdentity getIdentity()
    {
        return identity;
    }

    /**
     * @param identity sets the {@link #identity}
     */
    public void setIdentity(AclIdentity identity)
    {
        this.identity = identity;
    }

    /**
     * @return {@link #objectIdentity}
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "acl_object_identity_id")
    public AclObjectIdentity getObjectIdentity()
    {
        return objectIdentity;
    }

    /**
     * @param objectIdentity {@link #objectIdentity}
     */
    public void setObjectIdentity(AclObjectIdentity objectIdentity)
    {
        this.objectIdentity = objectIdentity;
    }

    /**
     * @return {@link #role}
     */
    @Column(name = "role", nullable = false, length = AbstractComplexType.ENUM_COLUMN_LENGTH)
    public String getRole()
    {
        return role;
    }

    /**
     * @param role sets the {@link #role}
     */
    public void setRole(String role)
    {
        this.role = role;
    }
}
