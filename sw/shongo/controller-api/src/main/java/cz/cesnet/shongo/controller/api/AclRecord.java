package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.rpc.StructType;
import cz.cesnet.shongo.controller.Role;

/**
 * Represents a record in Shongo ACL which means that user with specified {@link #user} has role
 * with specified {@link #role} for resource with specified {@link #entityId}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AclRecord implements StructType
{
    /**
     * Identifier of ACL record.
     */
    private String id;

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
     * @return {@link #id}
     */
    public String getId()
    {
        return id;
    }

    /**
     * @param id sets the {@link #id}
     */
    public void setId(String id)
    {
        this.id = id;
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
}
