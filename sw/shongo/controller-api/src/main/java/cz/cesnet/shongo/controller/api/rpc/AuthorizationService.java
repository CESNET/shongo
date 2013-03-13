package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.annotation.Transient;
import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.api.rpc.StructType;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.fault.EntityNotFoundException;
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
     * Create {@link UserResourceRole} for given parameters.
     *
     * @param token      token of the user requesting the operation
     * @param userId     identifier of the Shongo user
     * @param resourceId identifier of the Shongo public entity
     * @param roleId     identifier of the role
     * @return identifier of newly created ACL record
     */
    @API
    public String createUserResourceRole(SecurityToken token, String userId, String resourceId, String roleId) throws FaultException;

    /**
     * Delete {@link UserResourceRole} with given {@code id}.
     *
     * @param token token of the user requesting the operation
     * @param id    identifier of newly created ACL record
     */
    @API
    public void deleteUserResourceRole(SecurityToken token, String id) throws EntityNotFoundException;

    /**
     * Retrieve {@link UserResourceRole} with given {@code id}.
     *
     * @param token token of the user requesting the operation
     * @param id    identifier of newly created ACL record
     * @return {@link UserResourceRole} with given {@code id}
     */
    @API
    public UserResourceRole getUserResourceRole(SecurityToken token, String id) throws EntityNotFoundException;

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
     * @param userId of the user
     * @return {@link UserInformation} for given {@code userId}
     */
    @API
    public UserInformation getUser(SecurityToken token, String userId);

    /**
     * @param token token of the user requesting the operation
     * @param filter  for filtering or {@code null}
     * @return collection of {@link UserInformation}s that matches given {@code filter}
     */
    @API
    public Collection<UserInformation> listUsers(SecurityToken token, String filter);

    /**
     * Represents a record in Shongo ACL which means that user with specified {@link #user} has role
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
        UserInformation user;

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
         * @return {@link #user}
         */
        public UserInformation getUser()
        {
            return user;
        }

        /**
         * @return {@link cz.cesnet.shongo.api.UserInformation#getUserId()}
         */
        @Transient
        public String getUserId()
        {
            return user.getUserId();
        }

        /**
         * @param user sets the {@link #user}
         */
        public void setUser(UserInformation user)
        {
            this.user = user;
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
