package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.controller.AclIdentityType;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.api.AclEntry;

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

    private UserInformation user;

    private String objectId;

    private ObjectRole role;

    private boolean deletable = false;

    private CacheProvider cacheProvider;

    public UserRoleModel(UserInformation userInformation)
    {
        setUser(userInformation);
    }

    public UserRoleModel(CacheProvider cacheProvider)
    {
        this.cacheProvider = cacheProvider;
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

    public String getUserId()
    {
        if (user == null) {
            return null;
        }
        return user.getUserId();
    }

    public void setUserId(String userId)
    {
        if (userId == null || userId.isEmpty()) {
            setUser(null);
            return;
        }
        if (cacheProvider == null) {
            throw new IllegalStateException("UserInformationProvider isn't set.");
        }
        setUser(cacheProvider.getUserInformation(userId));
    }

    public UserInformation getUser()
    {
        return user;
    }

    public void setUser(UserInformation user)
    {
        this.user = user;
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
        if (!aclEntry.getIdentityType().equals(AclIdentityType.USER)) {
            throw new UnsupportedApiException(aclEntry.getIdentityType());
        }
        setUserId(aclEntry.getIdentityPrincipalId());
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
        aclEntry.setIdentityType(AclIdentityType.USER);
        aclEntry.setIdentityPrincipalId(user.getUserId());
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
        attributes.put("User", user);
        attributes.put("Role", role);
        return ReportModel.formatAttributes(attributes);
    }
}
