package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.api.AclRecord;
import cz.cesnet.shongo.controller.api.PermissionSet;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.UserSettings;
import cz.cesnet.shongo.controller.api.request.*;

import java.util.Map;

/**
 * Interface defining service for accessing Shongo ACL.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface AuthorizationService extends Service
{
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
     * List permissions of requesting user for entities.
     *
     * @param request {@link PermissionListRequest}
     * @return set of permissions for each requested entity
     */
    @API
    public Map<String, PermissionSet> listPermissions(PermissionListRequest request);

    /**
     * @param request {@link UserListRequest}
     * @return collection of {@link UserInformation}s that matches given {@code filter}
     */
    @API
    public ListResponse<UserInformation> listUsers(UserListRequest request);

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
     * @param userSettings to be updated for the requesting user
     */
    @API
    public void updateUserSettings(SecurityToken securityToken, UserSettings userSettings);
}
