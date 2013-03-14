package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.controller.Authorization;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.common.EntityIdentifier;

/**
* TODO:
*
* @author Martin Srom <martin.srom@cesnet.cz>
*/
public class AclRecord
{
    private static long debugId = 0;

    private final String id;

    private final String userId;

    private final EntityIdentifier entityId;

    private final Role role;

    public AclRecord(String userId, EntityIdentifier entityId, Role role)
    {
        this(String.valueOf(++debugId), userId, entityId, role);
    }

    public AclRecord(String id, String userId, EntityIdentifier entityId, Role role)
    {
        this.id = id;
        this.userId = userId;
        this.entityId = entityId;
        this.role = role;
    }

    public String getId()
    {
        return id;
    }

    public String getUserId()
    {
        return userId;
    }

    public EntityIdentifier getEntityId()
    {
        return entityId;
    }

    public Role getRole()
    {
        return role;
    }

    public cz.cesnet.shongo.controller.api.AclRecord toApi(Authorization authorization)
    {
        cz.cesnet.shongo.controller.api.AclRecord aclRecordApi = new cz.cesnet.shongo.controller.api.AclRecord();
        aclRecordApi.setId(getId());
        aclRecordApi.setUser(authorization.getUserInformation(getUserId()));
        aclRecordApi.setEntityId(getEntityId().toId());
        aclRecordApi.setRole(getRole());
        return aclRecordApi;
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
