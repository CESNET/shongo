package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.controller.Permission;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.api.AclRecord;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.fault.FaultException;

import java.util.Collection;

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
    public String createAclRecord(SecurityToken token, String userId, String entityId, Role role)
            throws FaultException;

    /**
     * Delete {@link cz.cesnet.shongo.controller.api.AclRecord} with given {@code id}.
     *
     * @param token       token of the user requesting the operation
     * @param aclRecordId identifier of newly created ACL record
     */
    @API
    public void deleteAclRecord(SecurityToken token, String aclRecordId)
            throws FaultException;

    /**
     * Retrieve {@link cz.cesnet.shongo.controller.api.AclRecord} with given {@code id}.
     *
     * @param token       token of the user requesting the operation
     * @param aclRecordId identifier of newly created ACL record
     * @return {@link cz.cesnet.shongo.controller.api.AclRecord} with given {@code id}
     */
    @API
    public AclRecord getAclRecord(SecurityToken token, String aclRecordId)
            throws FaultException;

    /**
     * Retrieve collection of {@link cz.cesnet.shongo.controller.api.AclRecord} for given parameters.
     *
     * @param token    token of the user requesting the operation
     * @param userId   identifier of the Shongo user
     * @param entityId identifier of the Shongo public entity
     * @param role     role which the user has for the entity
     * @return collection of {@link cz.cesnet.shongo.controller.api.AclRecord} that matches given parameters
     */
    @API
    public Collection<AclRecord> listAclRecords(SecurityToken token, String userId, String entityId, Role role)
            throws FaultException;


    /**
     * List permissions of user with given {@code token} for entity with given {@code entityId}.
     *
     * @param token    token of the user
     * @param entityId identifier of the Shongo public entity
     * @return collection of permissions
     */
    @API
    public Collection<Permission> listPermissions(SecurityToken token, String entityId)
            throws FaultException;

    /**
     * @param token  token of the user requesting the operation
     * @param userId of the user
     * @return {@link UserInformation} for given {@code userId}
     */
    @API
    public UserInformation getUser(SecurityToken token, String userId)
            throws FaultException;

    /**
     * @param token  token of the user requesting the operation
     * @param filter for filtering or {@code null}
     * @return collection of {@link UserInformation}s that matches given {@code filter}
     */
    @API
    public Collection<UserInformation> listUsers(SecurityToken token, String filter);
}
