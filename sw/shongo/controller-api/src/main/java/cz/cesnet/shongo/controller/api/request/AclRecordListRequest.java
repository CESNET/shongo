package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.api.SecurityToken;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link cz.cesnet.shongo.controller.api.request.ListRequest} for reservations.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AclRecordListRequest extends ListRequest
{
    /**
     * Identifiers of the ACL records.
     */
    private Set<String> aclRecordIds = new HashSet<String>();

    /**
     * Identifiers of the Shongo user.
     */
    private Set<String> userIds = new HashSet<String>();

    /**
     * Identifier of the Shongo public entity.
     */
    private Set<String> entityIds = new HashSet<String>();

    /**
     * Role which the user has for the entity.
     */
    private Set<Role> roles = new HashSet<Role>();

    /**
     * Constructor.
     */
    public AclRecordListRequest()
    {
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public AclRecordListRequest(SecurityToken securityToken)
    {
        super(securityToken);
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     * @param userId to be added to the {@link #userIds}
     */
    public AclRecordListRequest(SecurityToken securityToken, String userId)
    {
        super(securityToken);
        addUserId(userId);
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     * @param userId to be added to the {@link #userIds}
     */
    public AclRecordListRequest(SecurityToken securityToken, String userId, String entityId, Role role)
    {
        super(securityToken);
        addUserId(userId);
        addEntityId(entityId);
        addRole(role);
    }

    public Set<String> getAclRecordIds()
    {
        return aclRecordIds;
    }

    public void setAclRecordIds(Set<String> aclRecordIds)
    {
        this.aclRecordIds = aclRecordIds;
    }

    public void addAclRecordId(String aclRecordId)
    {
        aclRecordIds.add(aclRecordId);
    }

    public Set<String> getUserIds()
    {
        return userIds;
    }

    public void setUserIds(Set<String> userIds)
    {
        this.userIds = userIds;
    }

    public void addUserId(String userId)
    {
        userIds.add(userId);
    }

    public Set<String> getEntityIds()
    {
        return entityIds;
    }

    public void setEntityIds(Set<String> entityIds)
    {
        this.entityIds = entityIds;
    }

    public void addEntityId(String entityId)
    {
        entityIds.add(entityId);
    }

    public Set<Role> getRoles()
    {
        return roles;
    }

    public void setRoles(Set<Role> roles)
    {
        this.roles = roles;
    }

    public void addRole(Role role)
    {
        roles.add(role);
    }
}
