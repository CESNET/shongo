package cz.cesnet.shongo.controller.acl;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.TodoImplementException;

import javax.persistence.*;
import java.util.List;

/**
 * Represents an identity of object which can be used in {@link AclEntry}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"acl_object_class_id", "object_id"}))
@NamedQuery(name="AclObjectIdentity.find", query="SELECT i FROM AclObjectIdentity i WHERE i.objectClass = :objectClass AND i.objectId = :objectId")
public class AclObjectIdentity extends SimplePersistentObject
{
    /**
     * @see AclObjectClass
     */
    private AclObjectClass objectClass;

    /**
     * Unique identifier of object.
     */
    private Long objectId;

    private List<AclEntry> aclEntryList;

    /**
     * @return {@link #objectClass}
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "acl_object_class_id")
    public AclObjectClass getObjectClass()
    {
        return objectClass;
    }

    /**
     * @param objectClass sets the {@link #objectClass}
     */
    public void setObjectClass(AclObjectClass objectClass)
    {
        this.objectClass = objectClass;
    }

    /**
     * @return {@link #objectId}
     */
    @Column(name = "object_id", nullable = false)
    public Long getObjectId()
    {
        return objectId;
    }

    /**
     * @param objectId sets the {@link #objectId}
     */
    public void setObjectId(Long objectId)
    {
        this.objectId = objectId;
    }

    @OneToMany(mappedBy = "objectIdentity")
    public List<AclEntry> getAclEntryList() {
        return aclEntryList;
    }

    public void setAclEntryList(List<AclEntry> aclEntryList) {
        this.aclEntryList = aclEntryList;
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
        AclObjectIdentity that = (AclObjectIdentity) o;
        if (!objectClass.equals(that.objectClass)) {
            return false;
        }
        if (!objectId.equals(that.objectId)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = objectClass.hashCode();
        result = 31 * result + objectId.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return objectClass + ":" + objectId;
    }
}
