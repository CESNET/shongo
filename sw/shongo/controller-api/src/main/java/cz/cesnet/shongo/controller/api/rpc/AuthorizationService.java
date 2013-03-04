package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.api.rpc.StructType;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.fault.FaultException;

import java.util.Collection;

/**
 * Interface defining service for accessing Shongo ACLs.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface AuthorizationService extends Service
{
    /**
     * Create {@link UserResourceRole} for given parameters.
     *
     * @param token      token of the user requesting the operation
     * @param userId     identifier of the Shongo user
     * @param resourceId identifier of the Shongo public entity
     * @param roleId     identifier of the role
     * @return identifier of newly created ACL record
     */
    @API
    public String createUserResourceRole(SecurityToken token, String userId, String resourceId, String roleId);

    /**
     * Delete {@link UserResourceRole} with given {@code id}.
     *
     * @param token token of the user requesting the operation
     * @param id    identifier of newly created ACL record
     */
    @API
    public void deleteUserResourceRole(SecurityToken token, String id);

    /**
     * Retrieve {@link UserResourceRole} with given {@code id}.
     *
     * @param token token of the user requesting the operation
     * @param id    identifier of newly created ACL record
     * @return {@link UserResourceRole} with given {@code id}
     */
    @API
    public UserResourceRole getUserResourceRole(SecurityToken token, String id);

    /**
     * Retrieve collection of {@link UserResourceRole} for given parameters.
     *
     * @param token      token of the user requesting the operation
     * @param userId     identifier of the Shongo user
     * @param resourceId identifier of the Shongo public entity
     * @param roleId     identifier of the role
     * @return collection of {@link UserResourceRole} that matches given parameters
     */
    @API
    public Collection<UserResourceRole> listUserResourceRoles(SecurityToken token, String userId, String resourceId,
            String roleId) throws FaultException;

    /**
     * @param token token of the user requesting the operation
     * @param name  for filtering or {@code null}
     * @return collection of {@link UserInformation}s that matches given {@code name}
     */
    @API
    public Collection<UserInformation> listUsers(SecurityToken token, String name);

    /**
     * Represents a record in Shongo ACL which means that user with specified {@link #userId} has role
     * with specified {@link #roleId} for resource with specified {@link #resourceId}.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    public static class UserResourceRole implements StructType
    {
        /**
         * Identifier of ACL record.
         */
        String id;

        /**
         * Identifier of Shongo user.
         */
        String userId;

        /**
         * Global identifier of any Shongo public entity.
         */
        String resourceId;

        /**
         * Identifier of role.
         */
        String roleId;

        /**
         * @return {@link #id}
         */
        public String getId()
        {
            return id;
        }

        /**
         * @param id sets the {@link #id}
         */
        public void setId(String id)
        {
            this.id = id;
        }

        /**
         * @return {@link #userId}
         */
        public String getUserId()
        {
            return userId;
        }

        /**
         * @param userId sets the {@link #userId}
         */
        public void setUserId(String userId)
        {
            this.userId = userId;
        }

        /**
         * @return {@link #resourceId}
         */
        public String getResourceId()
        {
            return resourceId;
        }

        /**
         * @param resourceId sets the {@link #resourceId}
         */
        public void setResourceId(String resourceId)
        {
            this.resourceId = resourceId;
        }

        /**
         * @return {@link #roleId}
         */
        public String getRoleId()
        {
            return roleId;
        }

        /**
         * @param roleId {@link #roleId}
         */
        public void setRoleId(String roleId)
        {
            this.roleId = roleId;
        }
    }


}
