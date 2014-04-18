package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.controller.AclIdentityType;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.api.AclEntry;
import cz.cesnet.shongo.controller.api.Group;

import java.util.LinkedHashMap;
import java.util.Map;

/**
* TODO:
*
* @author Martin Srom <martin.srom@cesnet.cz>
*/
public class UserRoleModel implements ReportModel.ContextSerializable
{
    private String id;

    private AclIdentityType identityType;

    private String identityPrincipalId;

    private UserInformation user;

    private Group group;

    private String objectId;

    private ObjectRole role;

    private boolean deletable = false;

    private CacheProvider cacheProvider;

    public UserRoleModel(UserInformation userInformation)
    {
        this.identityType = AclIdentityType.USER;
        this.identityPrincipalId = userInformation.getUserId();
        this.user = userInformation;
    }

    public UserRoleModel(CacheProvider cacheProvider)
    {
        this.cacheProvider = cacheProvider;
        this.identityType = AclIdentityType.USER;
    }

    public UserRoleModel(AclEntry aclEntry, CacheProvider cacheProvider)
    {
        this.cacheProvider = cacheProvider;
        fromApi(aclEntry);
    }

    public boolean isNew()
    {
        return id == null || CommonModel.isNewId(id);
    }

    public String getId()
    {
        return id;
    }

    public void setNewId()
    {
        this.id = CommonModel.getNewId();
    }

    public AclIdentityType getIdentityType()
    {
        return identityType;
    }

    public void setIdentityType(AclIdentityType identityType)
    {
        if (!identityType.equals(this.identityType)) {
            user = null;
            group = null;
        }
        this.identityType = identityType;
    }

    public String getIdentityPrincipalId()
    {
        return identityPrincipalId;
    }

    public void setIdentityPrincipalId(String identityPrincipalId)
    {
        if (!identityPrincipalId.equals(this.identityPrincipalId)) {
            user = null;
            group = null;
        }
        this.identityPrincipalId = identityPrincipalId;
    }

    public String getIdentityName()
    {
        switch (identityType) {
            case USER:
                return getUser().getFullName();
            case GROUP:
                return getGroup().getName();
            default:
                throw new TodoImplementException(identityType);
        }
    }

    public UserInformation getUser()
    {
        if (user == null && identityType.equals(AclIdentityType.USER)) {
            if (cacheProvider == null) {
                throw new IllegalStateException("CacheProvider isn't set.");
            }
            user = cacheProvider.getUserInformation(identityPrincipalId);

        }
        return user;
    }

    public Group getGroup()
    {
        if (group == null && identityType.equals(AclIdentityType.GROUP)) {
            if (cacheProvider == null) {
                throw new IllegalStateException("CacheProvider isn't set.");
            }
            group = cacheProvider.getGroup(identityPrincipalId);
        }
        return group;
    }

    public String getObjectId()
    {
        return objectId;
    }

    public void setObjectId(String objectId)
    {
        this.objectId = objectId;
    }

    public ObjectRole getRole()
    {
        return role;
    }

    public void setRole(ObjectRole role)
    {
        this.role = role;
    }

    public boolean isDeletable()
    {
        return deletable;
    }

    public void setDeletable(boolean deletable)
    {
        this.deletable = deletable;
    }

    public void fromApi(AclEntry aclEntry)
    {
        this.id = aclEntry.getId();
        setIdentityType(aclEntry.getIdentityType());
        setIdentityPrincipalId(aclEntry.getIdentityPrincipalId());
        setObjectId(aclEntry.getObjectId());
        setRole(aclEntry.getRole());
        setDeletable(aclEntry.isDeletable());
    }

    public AclEntry toApi()
    {
        AclEntry aclEntry = new AclEntry();
        if (!isNew()) {
            aclEntry.setId(id);
        }
        aclEntry.setIdentityType(identityType);
        aclEntry.setIdentityPrincipalId(identityPrincipalId);
        aclEntry.setObjectId(objectId);
        aclEntry.setRole(role);
        return aclEntry;
    }

    @Override
    public String toContextString()
    {
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("Object", objectId);
        switch (identityType) {
            case USER:
                attributes.put("User", getUser());
                break;
            case GROUP:
                attributes.put("Group", getGroup());
                break;
        }
        attributes.put("Role", role);
        return ReportModel.formatAttributes(attributes);
    }
}
