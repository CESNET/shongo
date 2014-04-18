package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.api.AclEntry;
import cz.cesnet.shongo.controller.api.SecurityToken;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link ListRequest} for {@link AclEntry}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AclEntryListRequest extends ListRequest
{
    /**
     * Identifiers of the ACL entries.
     */
    private Set<String> entryIds = new HashSet<String>();

    /**
     * Identifiers of the Shongo user.
     */
    private Set<String> userIds = new HashSet<String>();

    /**
     * Identifier of the Shongo public objects.
     */
    private Set<String> objectIds = new HashSet<String>();

    /**
     * Role which the user has for the object.
     */
    private Set<ObjectRole> roles = new HashSet<ObjectRole>();

    /**
     * Constructor.
     */
    public AclEntryListRequest()
    {
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public AclEntryListRequest(SecurityToken securityToken)
    {
        super(securityToken);
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     * @param userId to be added to the {@link #userIds}
     */
    public AclEntryListRequest(SecurityToken securityToken, String userId)
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
    public AclEntryListRequest(SecurityToken securityToken, String userId, String objectId, ObjectRole objectRole)
    {
        super(securityToken);
        addUserId(userId);
        addObjectId(objectId);
        addRole(objectRole);
    }

    public Set<String> getEntryIds()
    {
        return entryIds;
    }

    public void setEntryIds(Set<String> entryIds)
    {
        this.entryIds = entryIds;
    }

    public void addAclEntryId(String aclEntryId)
    {
        entryIds.add(aclEntryId);
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

    public Set<String> getObjectIds()
    {
        return objectIds;
    }

    public void setObjectIds(Set<String> objectIds)
    {
        this.objectIds = objectIds;
    }

    public void addObjectId(String objectId)
    {
        objectIds.add(objectId);
    }

    public Set<ObjectRole> getRoles()
    {
        return roles;
    }

    public void setRoles(Set<ObjectRole> roles)
    {
        this.roles = roles;
    }

    public void addRole(ObjectRole objectRole)
    {
        roles.add(objectRole);
    }

    private static final String ENTRY_IDS = "entryIds";
    private static final String USER_IDS = "userIds";
    private static final String OBJECT_IDS = "objectIds";
    private static final String ROLES = "roles";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ENTRY_IDS, entryIds);
        dataMap.set(USER_IDS, userIds);
        dataMap.set(OBJECT_IDS, objectIds);
        dataMap.set(ROLES, roles);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        entryIds = dataMap.getSet(ENTRY_IDS, String.class);
        userIds = dataMap.getSet(USER_IDS, String.class);
        objectIds = dataMap.getSet(OBJECT_IDS, String.class);
        roles = dataMap.getSet(ROLES, ObjectRole.class);
    }
}
