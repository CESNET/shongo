package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import cz.cesnet.shongo.controller.ObjectRole;

/**
 * Represents a record in Shongo ACL which means that user with specified {@link #userId} has role
 * with specified {@link #role} for resource with specified {@link #objectId}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AclEntry extends IdentifiedComplexType
{
    /**
     * User-id of user of the {@link AclEntry}.
     */
    private String userId;

    /**
     * Identifier of object of some {@link cz.cesnet.shongo.controller.EntityType} for which the user
     * gets granted the {@link #role}.
     */
    private String objectId;

    /**
     * {@link cz.cesnet.shongo.controller.ObjectRole} which the user gets granted for the object with {@link #objectId}.
     */
    private ObjectRole role;

    /**
     * Specifies whether {@link AclEntry} is not referenced by another {@link AclEntry} and thus it can be deleted.
     */
    private boolean deletable;

    /**
     * Constructor.
     */
    public AclEntry()
    {
    }

    /**
     * Constructor.
     *
     * @param userId   sets the {@link #userId}
     * @param objectId sets the {@link #objectId}
     * @param role     sets the {@link #role}
     */
    public AclEntry(String userId, String objectId, ObjectRole role)
    {
        this.userId = userId;
        this.objectId = objectId;
        this.role = role;
    }

    /**
     * @return {@link #userId}
     */
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    /**
     * @return {@link #objectId}
     */
    public String getObjectId()
    {
        return objectId;
    }

    /**
     * @param objectId sets the {@link #objectId}
     */
    public void setObjectId(String objectId)
    {
        this.objectId = objectId;
    }

    /**
     * @return {@link #role}
     */
    public ObjectRole getRole()
    {
        return role;
    }

    /**
     * @param role {@link #role}
     */
    public void setRole(ObjectRole role)
    {
        this.role = role;
    }

    /**
     * @return {@link #deletable}
     */
    public boolean isDeletable()
    {
        return deletable;
    }

    /**
     * @param deletable sets the {@link #deletable}
     */
    public void setDeletable(boolean deletable)
    {
        this.deletable = deletable;
    }

    @Override
    public String toString()
    {
        return String.format("AclEntry (user: %s, object: %s, role: %s)", userId, objectId, role);
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

        AclEntry record = (AclEntry) o;

        if (objectId != null ? !objectId.equals(record.objectId) : record.objectId != null) {
            return false;
        }
        if (role != record.role) {
            return false;
        }
        if (userId != null ? !userId.equals(record.userId) : record.userId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (objectId != null ? objectId.hashCode() : 0);
        result = 31 * result + (role != null ? role.hashCode() : 0);
        return result;
    }

    private static final String USER_ID = "userId";
    private static final String OBJECT_ID = "objectId";
    private static final String ROLE = "role";
    private static final String DELETABLE = "deletable";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USER_ID, userId);
        dataMap.set(OBJECT_ID, objectId);
        dataMap.set(ROLE, role);
        dataMap.set(DELETABLE, deletable);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        userId = dataMap.getString(USER_ID);
        objectId = dataMap.getString(OBJECT_ID);
        role = dataMap.getEnum(ROLE, ObjectRole.class);
        deletable = dataMap.getBool(DELETABLE);
    }
}
