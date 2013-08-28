package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import cz.cesnet.shongo.controller.Role;

/**
 * Represents a record in Shongo ACL which means that user with specified {@link #userId} has role
 * with specified {@link #role} for resource with specified {@link #entityId}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AclRecord extends IdentifiedComplexType
{
    /**
     * User-id of user of the {@link AclRecord}.
     */
    private String userId;

    /**
     * Identifier of entity of some {@link cz.cesnet.shongo.controller.EntityType} for which the user
     * gets granted the {@link #role}.
     */
    private String entityId;

    /**
     * {@link cz.cesnet.shongo.controller.Role} which the user gets granted for the entity with {@link #entityId}.
     */
    private Role role;

    /**
     * Specifies whether {@link AclRecord} is not referenced by another {@link AclRecord} and thus it can be deleted.
     */
    private boolean deletable;

    /**
     * Constructor.
     */
    public AclRecord()
    {
    }

    /**
     * Constructor.
     *
     * @param userId   sets the {@link #userId}
     * @param entityId sets the {@link #entityId}
     * @param role     sets the {@link #role}
     */
    public AclRecord(String userId, String entityId, Role role)
    {
        this.userId = userId;
        this.entityId = entityId;
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
     * @return {@link #entityId}
     */
    public String getEntityId()
    {
        return entityId;
    }

    /**
     * @param entityId sets the {@link #entityId}
     */
    public void setEntityId(String entityId)
    {
        this.entityId = entityId;
    }

    /**
     * @return {@link #role}
     */
    public Role getRole()
    {
        return role;
    }

    /**
     * @param role {@link #role}
     */
    public void setRole(Role role)
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
        return String.format("AclRecord (user: %s, entity: %s, role: %s)", userId, entityId, role);
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

        AclRecord record = (AclRecord) o;

        if (entityId != null ? !entityId.equals(record.entityId) : record.entityId != null) {
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
        result = 31 * result + (entityId != null ? entityId.hashCode() : 0);
        result = 31 * result + (role != null ? role.hashCode() : 0);
        return result;
    }

    private static final String USER_ID = "userId";
    private static final String ENTITY_ID = "entityId";
    private static final String ROLE = "role";
    private static final String DELETABLE = "deletable";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USER_ID, userId);
        dataMap.set(ENTITY_ID, entityId);
        dataMap.set(ROLE, role);
        dataMap.set(DELETABLE, deletable);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        userId = dataMap.getString(USER_ID);
        entityId = dataMap.getString(ENTITY_ID);
        role = dataMap.getEnum(ROLE, Role.class);
        deletable = dataMap.getBool(DELETABLE);
    }
}
