package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.SystemPermission;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface defining service for accessing Shongo ACL.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface AuthorizationService extends Service
{
    /**
     * @param securityToken    token of the user requesting the operation
     * @param systemPermission to be checked
     * @return true whether requesting user has given {@code systemPermission},
     *         false otherwise
     */
    @API
    public boolean hasSystemPermission(SecurityToken securityToken, SystemPermission systemPermission);

    /**
     * @param securityToken token of the user requesting the operation
     * @return set of {@link SystemPermission}s which the requesting user has
     */
    @API
    public SystemPermissionSet getSystemPermissions(SecurityToken securityToken);

    /**
     * @param request {@link UserListRequest}
     * @return {@link ListResponse} of {@link UserInformation}s
     */
    @API
    public ListResponse<UserInformation> listUsers(UserListRequest request);

    /**
     * @param securityToken token of the user requesting the operation
     * @return {@link UserSettings} for the requesting user
     */
    @API
    public UserSettings getUserSettings(SecurityToken securityToken);

    /**
     * @param securityToken token of the user requesting the operation
     * @param useWebService specifies if the web service shall or shall not be used to return attributes in {@link UserSettings}
     * @return {@link UserSettings} for the requesting user
     */
    @API
    public UserSettings getUserSettings(SecurityToken securityToken, boolean useWebService);

    /**
     * @param securityToken token of the user requesting the operation
     * @param userId for user to get his settings
     * @return {@link UserSettings} for user with the given {@code userId}
     */
    @API
    public UserSettings getUserSettings(SecurityToken securityToken, String userId);

    /**
     * @param securityToken token of the user requesting the operation
     * @param userSettings  to be updated for the requesting user
     */
    @API
    public void updateUserSettings(SecurityToken securityToken, UserSettings userSettings);

    /**
     * @param securityToken token of the user requesting the operation
     * @param userId for user to update his settings
     * @param userSettings  to be updated for user with the given {@code userId}
     */
    @API
    public void updateUserSettings(SecurityToken securityToken, String userId, UserSettings userSettings);

    /**
     * @param securityToken token of the user requesting the operation
     * @param oldUserId     old user id
     * @param newUserId     new user id
     */
    @API
    public void modifyUserId(SecurityToken securityToken, String oldUserId, String newUserId);

    /**
     * @param request {@link GroupListRequest}
     * @return {@link ListResponse} of {@link Group}s
     */
    @API
    public ListResponse<Group> listGroups(GroupListRequest request);

    /**
     * @param token   token of the user requesting the operation
     * @param groupId of the group to be deleted
     * @return {@link Group} with given {@code #groupId}
     */
    @API
    public Group getGroup(SecurityToken token, String groupId);

    /**
     * @param token token of the user requesting the operation
     * @param group
     * @return identifier of the new group
     */
    @API
    public String createGroup(SecurityToken token, Group group);

    /**
     * @param token token of the user requesting the operation
     * @param group to be modified
     */
    @API
    public void modifyGroup(SecurityToken token, Group group);

    /**
     * @param token   token of the user requesting the operation
     * @param groupId of the group to be deleted
     */
    @API
    public void deleteGroup(SecurityToken token, String groupId);

    /**
     * @param token   token of the user requesting the operation
     * @param groupId of the group to which the user should be added
     * @param userId  of the user to be added
     */
    @API
    public void addGroupUser(SecurityToken token, String groupId, String userId);

    /**
     * @param token   token of the user requesting the operation
     * @param groupId of the group from which the user should be removed
     * @param userId  of the user to be removed
     */
    @API
    public void removeGroupUser(SecurityToken token, String groupId, String userId);

    /**
     * Create {@link cz.cesnet.shongo.controller.api.AclEntry} for given parameters.
     *
     *
     *
     * @param token      token of the user requesting the operation
     * @param aclEntry   to be created
     * @return identifier of newly created ACL entry
     */
    @API
    public String createAclEntry(SecurityToken token, AclEntry aclEntry);

    /**
     * Delete {@link cz.cesnet.shongo.controller.api.AclEntry} with given {@code id}.
     *
     * @param token       token of the user requesting the operation
     * @param aclEntryId identifier of newly created ACL entry
     */
    @API
    public void deleteAclEntry(SecurityToken token, String aclEntryId);

    /**
     * Retrieve collection of {@link cz.cesnet.shongo.controller.api.AclEntry} for given parameters.
     *
     * @param request {@link cz.cesnet.shongo.controller.api.request.AclEntryListRequest}
     * @return collection of {@link cz.cesnet.shongo.controller.api.AclEntry} that matches given parameters
     */
    @API
    public ListResponse<AclEntry> listAclEntries(AclEntryListRequest request);

    /**
     * List {@link ObjectPermission}s of requesting user for objects.
     *
     * @param request {@link ObjectPermissionListRequest}
     * @return set of permissions for each requested object
     */
    @API
    public Map<String, ObjectPermissionSet> listObjectPermissions(ObjectPermissionListRequest request);

    /**
     * @param token     token of the user requesting the operation
     * @param objectId  of the object
     * @param newUserId new user-id for the given {@code objectId}
     */
    @API
    public void setObjectUser(SecurityToken token, String objectId, String newUserId);

    /**
     * @param securityToken token of the user requesting the operation
     * @return list of {@link ReferencedUser}s
     */
    @API
    public List<ReferencedUser> listReferencedUsers(SecurityToken securityToken);
}
