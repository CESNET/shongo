package cz.cesnet.shongo.controller.rest.models.roles;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.AclIdentityType;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.api.AclEntry;
import cz.cesnet.shongo.controller.api.Group;
import cz.cesnet.shongo.controller.rest.CacheProvider;
import cz.cesnet.shongo.controller.rest.models.CommonModel;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents entity's role for specified resource.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @author Filip Karnis
 */
@Data
@NoArgsConstructor
public class UserRoleModel
{
    private String id;

    @JsonProperty("type")
    private AclIdentityType identityType;

    @JsonProperty("entityId")
    private String identityPrincipalId;

    private String identityName;

    private String identityDescription;

    private String email;

    private String objectId;

    private ObjectRole role;

    private boolean deletable = false;

    @JsonIgnore
    private CacheProvider cacheProvider;

    @JsonIgnore
    private UserInformation user;

    @JsonIgnore
    private Group group;

    public UserRoleModel(UserInformation userInformation)
    {
        this.identityType = AclIdentityType.USER;
        this.identityPrincipalId = userInformation.getUserId();
        this.user = userInformation;
    }

    public UserRoleModel(CacheProvider cacheProvider, AclIdentityType type)
    {
        this.cacheProvider = cacheProvider;
        this.identityType = type;
    }

    public UserRoleModel(AclEntry aclEntry, CacheProvider cacheProvider)
    {
        this.cacheProvider = cacheProvider;
        fromApi(aclEntry);
    }

    private boolean isNew()
    {
        return id == null || CommonModel.isNewId(id);
    }

    public void setNewId()
    {
        this.id = CommonModel.getNewId();
    }

    public void setIdentityType(AclIdentityType identityType)
    {
        if (!identityType.equals(this.identityType)) {
            user = null;
            group = null;
        }
        this.identityType = identityType;
    }

    public void setIdentityPrincipalId(String identityPrincipalId)
    {
        if (!identityPrincipalId.equals(this.identityPrincipalId)) {
            user = null;
            group = null;
        }
        this.identityPrincipalId = identityPrincipalId;
    }

    private String getIdentityNameFromApi()
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

    private String getIdentityDescriptionFromApi()
    {
        switch (identityType) {
            case USER:
                return getUser().getOrganization();
            case GROUP:
                return getGroup().getDescription();
            default:
                throw new TodoImplementException(identityType);
        }
    }

    @JsonIgnore
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

    private Group getGroup()
    {
        if (group == null && identityType.equals(AclIdentityType.GROUP)) {
            if (cacheProvider == null) {
                throw new IllegalStateException("CacheProvider isn't set.");
            }
            group = cacheProvider.getGroup(identityPrincipalId);
        }
        return group;
    }

    public void fromApi(AclEntry aclEntry)
    {
        this.id = aclEntry.getId();
        setIdentityType(aclEntry.getIdentityType());
        setIdentityPrincipalId(aclEntry.getIdentityPrincipalId());
        setIdentityName(getIdentityNameFromApi());
        setIdentityDescription(getIdentityDescriptionFromApi());
        if (AclIdentityType.USER.equals(identityType)) {
            setEmail(getUser().getEmail());
        }
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
}
