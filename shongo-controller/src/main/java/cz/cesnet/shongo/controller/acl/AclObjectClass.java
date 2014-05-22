package cz.cesnet.shongo.controller.acl;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.api.AbstractComplexType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQuery;

/**
 * Represents a class of objects which can have a {@link AclObjectIdentity}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@NamedQuery(name="AclObjectClass.find", query="SELECT c FROM AclObjectClass c WHERE c.className = :className")
public class AclObjectClass extends SimplePersistentObject
{
    /**
     * Class name of object.
     */
    private String className;

    /**
     * @return {@link #className}
     */
    @Column(name = "class", nullable = false, unique = true, length = AbstractComplexType.ENUM_COLUMN_LENGTH)
    public String getClassName()
    {
        return className;
    }

    /**
     * @param className sets the {@link #className}
     */
    public void setClassName(String className)
    {
        this.className = className;
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
        AclObjectClass that = (AclObjectClass) o;
        if (!className.equals(that.className)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        return className.hashCode();
    }

    @Override
    public String toString()
    {
        return className;
    }
}
