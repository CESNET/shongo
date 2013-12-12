package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.controller.EntityRole;
import cz.cesnet.shongo.controller.api.AclRecord;

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

    private String entityId;

    private EntityRole entityRole;

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

    public UserRoleModel(AclRecord aclRecord, CacheProvider cacheProvider)
    {
        this.cacheProvider = cacheProvider;
        fromApi(aclRecord);
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

    public String getEntityId()
    {
        return entityId;
    }

    public void setEntityId(String entityId)
    {
        this.entityId = entityId;
    }

    public EntityRole getEntityRole()
    {
        return entityRole;
    }

    public void setEntityRole(EntityRole entityRole)
    {
        this.entityRole = entityRole;
    }

    public boolean isDeletable()
    {
        return deletable;
    }

    public void setDeletable(boolean deletable)
    {
        this.deletable = deletable;
    }

    public void fromApi(AclRecord aclRecord)
    {
        this.id = aclRecord.getId();
        setUserId(aclRecord.getUserId());
        setEntityId(aclRecord.getEntityId());
        setEntityRole(aclRecord.getEntityRole());
        setDeletable(aclRecord.isDeletable());
    }

    public AclRecord toApi()
    {
        AclRecord aclRecord = new AclRecord();
        if (!isNew()) {
            aclRecord.setId(id);
        }
        aclRecord.setUserId(user.getUserId());
        aclRecord.setEntityId(entityId);
        aclRecord.setEntityRole(entityRole);
        return aclRecord;
    }

    @Override
    public String toContextString()
    {
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("Entity", entityId);
        attributes.put("User", user);
        attributes.put("Role", entityRole);
        return ReportModel.formatAttributes(attributes);
    }
}
