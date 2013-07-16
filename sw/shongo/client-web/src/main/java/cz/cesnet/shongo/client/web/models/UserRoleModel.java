package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.auth.UserInformationProvider;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.api.AclRecord;

/**
* TODO:
*
* @author Martin Srom <martin.srom@cesnet.cz>
*/
public class UserRoleModel
{
    private String id;

    private String entityId;

    private UserInformation user;

    private Role role;

    private UserInformationProvider userInformationProvider;

    private static int lastGeneratedId = 0;

    public UserRoleModel(UserInformation userInformation)
    {
        setUser(userInformation);
    }

    public UserRoleModel(UserInformationProvider userInformationProvider)
    {
        this.userInformationProvider = userInformationProvider;
    }

    public UserRoleModel(AclRecord aclRecord, UserInformationProvider userInformationProvider)
    {
        this.userInformationProvider = userInformationProvider;
        fromApi(aclRecord);
    }

    public String getId()
    {
        return id;
    }

    public void setTemporaryId()
    {
        synchronized (UserRoleModel.class) {
            id = "new-" + String.valueOf(++lastGeneratedId);
        }
    }

    private void setId(String id)
    {
        this.id = id;
    }

    public String getEntityId()
    {
        return entityId;
    }

    public void setEntityId(String entityId)
    {
        this.entityId = entityId;
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
        if (userInformationProvider == null) {
            throw new IllegalStateException("UserInformationProvider isn't set.");
        }
        setUser(userInformationProvider.getUserInformation(userId));
    }

    public UserInformation getUser()
    {
        return user;
    }

    public void setUser(UserInformation user)
    {
        this.user = user;
    }

    public Role getRole()
    {
        return role;
    }

    public void setRole(Role role)
    {
        this.role = role;
    }

    public void fromApi(AclRecord aclRecord)
    {
        setId(aclRecord.getId());
        setUserId(aclRecord.getUserId());
        setEntityId(aclRecord.getEntityId());
        setRole(aclRecord.getRole());
    }

    public AclRecord toApi()
    {
        AclRecord aclRecord = new AclRecord();
        if (!id.startsWith("new-")) {
            aclRecord.setId(id);
        }
        aclRecord.setUserId(user.getUserId());
        aclRecord.setEntityId(entityId);
        aclRecord.setRole(role);
        return aclRecord;
    }
}
