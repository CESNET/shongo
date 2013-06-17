package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.Permission;
import cz.cesnet.shongo.controller.api.PermissionSet;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.PermissionListRequest;
import cz.cesnet.shongo.controller.api.request.UserListRequest;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import org.joda.time.Duration;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Cache of {@link UserInformation}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserCache
{
    @Resource
    private AuthorizationService authorizationService;

    /**
     * {@link UserInformation}s by user-ids.
     */
    private ExpirationMap<String, UserInformation> userInformationByUserId =
            new ExpirationMap<String, UserInformation>();

    /**
     * {@link UserState}s by {@link SecurityToken}.
     */
    private ExpirationMap<SecurityToken, UserState> userStateByToken = new ExpirationMap<SecurityToken, UserState>();

    /**
     * Cached information for single user.
     */
    private static class UserState
    {
        /**
         * Set of permissions which the user has for entity.
         */
        private ExpirationMap<String, Set<Permission>> permissionsByEntity =
                new ExpirationMap<String, Set<Permission>>();

        /**
         * Constructor.
         */
        public UserState()
        {
            permissionsByEntity.setExpiration(Duration.standardMinutes(5));
        }
    }

    /**
     * Constructor.
     */
    public UserCache()
    {
        userInformationByUserId.setExpiration(Duration.standardMinutes(5));
        userStateByToken.setExpiration(Duration.standardHours(1));
    }

    /**
     * @param securityToken to be used for fetching the {@link UserInformation}
     * @param userId        user-id of the requested user
     * @return {@link UserInformation} for given {@code userId}
     */
    public synchronized UserInformation getUserInformation(SecurityToken securityToken, String userId)
    {
        UserInformation userInformation = userInformationByUserId.get(userId);
        if (userInformation == null) {
            ListResponse<UserInformation> response = authorizationService.listUsers(
                    new UserListRequest(securityToken, userId));
            if (response.getCount() == 0) {
                throw new RuntimeException("User with id '" + userId + "' hasn't been found.");
            }
            userInformation = response.getItem(0);
            userInformationByUserId.put(userId, userInformation);
        }
        return userInformation;
    }

    /**
     * @param securityToken
     * @return {@link UserState} for user with given {@code securityToken}
     */
    private synchronized UserState getUserState(SecurityToken securityToken)
    {
        UserState userState = userStateByToken.get(securityToken);
        if (userState == null) {
            userState = new UserState();
            userStateByToken.put(securityToken, userState);
        }
        return userState;
    }

    /**
     * @param securityToken of the requesting user
     * @param entityId      of the entity
     * @return set of {@link Permission} for requesting user and given {@code entityId}
     */
    public synchronized Set<Permission> getPermissions(SecurityToken securityToken, String entityId)
    {
        UserState userState = getUserState(securityToken);
        Set<Permission> permissions = userState.permissionsByEntity.get(entityId);
        if (permissions == null) {
            Map<String, PermissionSet> permissionsByEntity =
                    authorizationService.listPermissions(new PermissionListRequest(securityToken, entityId));
            permissions = new HashSet<Permission>();
            permissions.addAll(permissionsByEntity.get(entityId).getPermissions());
            userState.permissionsByEntity.put(entityId, permissions);
        }
        return permissions;
    }

    /**
     * @param securityToken of the requesting user
     * @param entityId      of the entity
     * @return set of {@link Permission} for requesting user and given {@code entityId}
     *         or null if the {@link Permission}s aren't cached
     */
    public synchronized Set<Permission> getPermissionsWithoutFetching(SecurityToken securityToken, String entityId)
    {
        UserState userState = getUserState(securityToken);
        return userState.permissionsByEntity.get(entityId);
    }

    /**
     * Fetch {@link Permission}s for given {@code entityIds}.
     *
     * @param securityToken
     * @param entityIds
     * @return fetched {@link Permission}s by {@code entityIds}
     */
    public synchronized Map<String, Set<Permission>> fetchPermissions(SecurityToken securityToken,
            Set<String> entityIds)
    {
        UserState userState = getUserState(securityToken);
        Map<String, PermissionSet> permissionsByEntity =
                authorizationService.listPermissions(new PermissionListRequest(securityToken, entityIds));
        Map<String, Set<Permission>> result = new HashMap<String, Set<Permission>>();
        for (Map.Entry<String, PermissionSet> entry : permissionsByEntity.entrySet()) {
            String entityId = entry.getKey();
            Set<Permission> permissions = userState.permissionsByEntity.get(entityId);
            if (permissions == null) {
                permissions = new HashSet<Permission>();
                userState.permissionsByEntity.put(entityId, permissions);
            }
            permissions.clear();
            permissions.addAll(entry.getValue().getPermissions());
            result.put(entityId, permissions);
        }
        return result;
    }
}
