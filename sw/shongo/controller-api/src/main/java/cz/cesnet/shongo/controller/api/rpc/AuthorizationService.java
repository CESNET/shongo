package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.controller.EntityPermission;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.AclRecordListRequest;
import cz.cesnet.shongo.controller.api.request.EntityPermissionListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.UserListRequest;

import java.util.List;
import java.util.Map;

/**
 * Interface defining service for accessing Shongo ACL.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface AuthorizationService extends Service
{
    /**
     * @param request {@link UserListRequest}
     * @return collection of {@link UserInformation}s that matches given {@code filter}
     */
    @API
    public ListResponse<UserInformation> listUsers(UserListRequest request);

    /**
     * @param token token of the user requesting the operation
     * @return collection of {@link UserInformation}s that matches given {@code filter}
     */
    @API
    public List<Group> listGroups(SecurityToken token);

    /**
     * @param token token of the user requesting the operation
     * @param group
     * @return identifier of the new group
     */
    @API
    public String createGroup(SecurityToken token, Group group);

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
     * Create {@link cz.cesnet.shongo.controller.api.AclRecord} for given parameters.
     *
     * @param token    token of the user requesting the operation
     * @param userId   identifier of the Shongo user
     * @param entityId identifier of the Shongo public entity
     * @param role     role which the user gets granted for the entity
     * @return identifier of newly created ACL record
     */
    @API
    public String createAclRecord(SecurityToken token, String userId, String entityId, Role role);

    /**
     * Delete {@link cz.cesnet.shongo.controller.api.AclRecord} with given {@code id}.
     *
     * @param token       token of the user requesting the operation
     * @param aclRecordId identifier of newly created ACL record
     */
    @API
    public void deleteAclRecord(SecurityToken token, String aclRecordId);

    /**
     * Retrieve collection of {@link cz.cesnet.shongo.controller.api.AclRecord} for given parameters.
     *
     * @param request {@link AclRecordListRequest}
     * @return collection of {@link cz.cesnet.shongo.controller.api.AclRecord} that matches given parameters
     */
    @API
    public ListResponse<AclRecord> listAclRecords(AclRecordListRequest request);

    /**
     * List {@link EntityPermission}s of requesting user for entities.
     *
     * @param request {@link EntityPermissionListRequest}
     * @return set of permissions for each requested entity
     */
    @API
    public Map<String, EntityPermissionSet> listEntityPermissions(EntityPermissionListRequest request);

    /**
     * @param token     token of the user requesting the operation
     * @param entityId  of the entity
     * @param newUserId new user-id for the given {@code entityId}
     */
    @API
    public void setEntityUser(SecurityToken token, String entityId, String newUserId);

    /**
     * @param securityToken token of the user requesting the operation
     * @return {@link UserSettings} for the requesting user
     */
    @API
    public UserSettings getUserSettings(SecurityToken securityToken);

    /**
     * @param securityToken token of the user requesting the operation
     * @param userSettings  to be updated for the requesting user
     */
    @API
    public void updateUserSettings(SecurityToken securityToken, UserSettings userSettings);

    /**
     * @param securityToken token of the user requesting the operation
     * @param oldUserId     old user id
     * @param newUserId     new user id
     */
    @API
    public void modifyUserId(SecurityToken securityToken, String oldUserId, String newUserId);

    /**
     * @param securityToken token of the user requesting the operation
     * @return map of user-id and description how the user is referenced
     */
    @API
    public Map<String, String> listReferencedUsers(SecurityToken securityToken);
}
