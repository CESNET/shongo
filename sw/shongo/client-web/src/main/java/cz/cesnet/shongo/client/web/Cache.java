package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.Permission;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.PermissionListRequest;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.request.UserListRequest;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Cache of {@link UserInformation}s, {@link Permission}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Cache
{
    private static Logger logger = LoggerFactory.getLogger(Cache.class);

    /**
     * Expiration of user information/permissions in minutes.
     */
    private static final long USER_EXPIRATION_MINUTES = 5;

    /**
     * Expiration of reservation requests.
     */
    private static final long RESERVATION_REQUEST_EXPIRATION_MINUTES = 5;

    @Resource
    private AuthorizationService authorizationService;

    @Resource
    private ReservationService reservationService;

    @Resource
    private ExecutableService executableService;

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
     * {@link ReservationRequestSummary} by identifier.
     */
    private ExpirationMap<String, ReservationRequestSummary> reservationRequestById =
            new ExpirationMap<String, ReservationRequestSummary>();

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
            permissionsByEntity.setExpiration(Duration.standardMinutes(USER_EXPIRATION_MINUTES));
        }
    }

    /**
     * Constructor.
     */
    public Cache()
    {
        userInformationByUserId.setExpiration(Duration.standardMinutes(USER_EXPIRATION_MINUTES));
        userStateByToken.setExpiration(Duration.standardHours(1));
        reservationRequestById.setExpiration(Duration.standardMinutes(RESERVATION_REQUEST_EXPIRATION_MINUTES));
    }

    /**
     * Method called each 5 minutes to clear expired items.
     */
    @Scheduled(fixedDelay = (USER_EXPIRATION_MINUTES * 60 * 1000))
    public synchronized void clearExpired()
    {
        logger.debug("Clearing expired user cache...");
        DateTime dateTimeNow = DateTime.now();
        userInformationByUserId.clearExpired(dateTimeNow);
        userStateByToken.clearExpired(dateTimeNow);
        for (UserState userState : userStateByToken) {
            userState.permissionsByEntity.clearExpired(dateTimeNow);
        }
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
     * @param permission    to be checked for existence
     * @return true if the requesting user has given {@code permission} for entity with given {@code entityId}
     */
    public boolean hasPermission(SecurityToken securityToken, String entityId, Permission permission)
    {
        Set<Permission> permissions = getPermissions(securityToken, entityId);
        return permissions.contains(permission);
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
        Map<String, Set<Permission>> result = new HashMap<String, Set<Permission>>();
        if (entityIds.isEmpty()) {
            return result;
        }
        UserState userState = getUserState(securityToken);
        Map<String, PermissionSet> permissionsByEntity =
                authorizationService.listPermissions(new PermissionListRequest(securityToken, entityIds));
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

    /**
     * Load {@link ReservationRequestSummary}s for given {@code reservationRequestIds} to the {@link Cache}.
     *
     * @param securityToken
     * @param reservationRequestIds
     */
    public synchronized void fetchReservationRequests(SecurityToken securityToken, Set<String> reservationRequestIds)
    {
        Set<String> missingReservationRequestIds = null;
        for (String reservationRequestId : reservationRequestIds) {
            if (!reservationRequestById.contains(reservationRequestId)) {
                if (missingReservationRequestIds == null) {
                    missingReservationRequestIds = new HashSet<String>();
                }
                missingReservationRequestIds.add(reservationRequestId);
            }
        }
        if (missingReservationRequestIds != null) {
            ReservationRequestListRequest request = new ReservationRequestListRequest();
            request.setSecurityToken(securityToken);
            request.setReservationRequestIds(missingReservationRequestIds);
            ListResponse<ReservationRequestSummary> response = reservationService.listReservationRequests(request);
            for (ReservationRequestSummary reservationRequest : response) {
                reservationRequestById.put(reservationRequest.getId(), reservationRequest);
            }
        }
    }

    /**
     * @param securityToken
     * @param reservationRequestId
     * @return {@link ReservationRequestSummary} for given {@code reservationRequestId}
     */
    public synchronized ReservationRequestSummary getReservationRequestSummary(SecurityToken securityToken,
            String reservationRequestId)
    {
        ReservationRequestSummary reservationRequest = reservationRequestById.get(reservationRequestId);
        if (reservationRequest == null) {
            reservationRequest = getReservationRequestSummaryNotCached(securityToken, reservationRequestId);
        }
        return reservationRequest;
    }

    /**
     * @param securityToken
     * @param reservationRequestId
     * @return {@link ReservationRequestSummary} for given {@code reservationRequestId}
     */
    public synchronized ReservationRequestSummary getReservationRequestSummaryNotCached(SecurityToken securityToken,
            String reservationRequestId)
    {
        ReservationRequestListRequest request = new ReservationRequestListRequest();
        request.setSecurityToken(securityToken);
        request.addReservationRequestId(reservationRequestId);
        ListResponse<ReservationRequestSummary> response = reservationService.listReservationRequests(request);
        if (response.getItemCount() > 0) {
            ReservationRequestSummary reservationRequest = response.getItem(0);
            reservationRequestById.put(reservationRequest.getId(), reservationRequest);
            return reservationRequest;
        }
        return null;
    }

    /**
     * @param securityToken
     * @param reservationId
     * @return {@link Reservation} for given {@code reservationId}
     */
    public Reservation getReservation(SecurityToken securityToken, String reservationId)
    {
        return reservationService.getReservation(securityToken, reservationId);
    }

    /**
     * @param securityToken
     * @param executable
     * @return reservation request id for given {@code executable}
     */
    public String getReservationRequestIdByExecutable(SecurityToken securityToken, Executable executable)
    {
        Reservation reservation = getReservation(securityToken, executable.getReservationId());
        return reservation.getReservationRequestId();
    }

    /**
     * @param securityToken
     * @param executableId
     * @return reservation request id for given {@code executableId}
     */
    public String getReservationRequestIdByExecutableId(SecurityToken securityToken, String executableId)
    {
        Executable executable = executableService.getExecutable(securityToken, executableId);
        return getReservationRequestIdByExecutable(securityToken, executable);
    }
}
