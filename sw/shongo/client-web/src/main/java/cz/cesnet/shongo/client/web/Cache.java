package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.SystemPermission;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.*;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.util.*;

/**
 * Cache of {@link UserInformation}s, {@link ObjectPermission}s, {@link ReservationRequestSummary}s.
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

    @Resource
    private AuthorizationService authorizationService;

    @Resource
    private ReservationService reservationService;

    @Resource
    private ExecutableService executableService;

    /**
     * {@link UserInformation}s by user-ids.
     */
    private ExpirationMap<SecurityToken, Map<SystemPermission, Boolean>> systemPermissionsByToken =
            new ExpirationMap<SecurityToken, Map<SystemPermission, Boolean>>();

    /**
     * {@link UserInformation}s by user-ids.
     */
    private ExpirationMap<String, UserInformation> userInformationByUserId =
            new ExpirationMap<String, UserInformation>();

    /**
     * {@link UserInformation}s by group-ids.
     */
    private ExpirationMap<String, Group> groupByGroupId =
            new ExpirationMap<String, Group>();

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
     * {@link Reservation} by identifier.
     */
    private ExpirationMap<String, Reservation> reservationById =
            new ExpirationMap<String, Reservation>();

    /**
     * {@link Reservation} by identifier.
     */
    private ExpirationMap<String, Executable> executableById =
            new ExpirationMap<String, Executable>();

    /**
     * Cached information for single user.
     */
    private static class UserState
    {
        /**
         * Set of permissions which the user has for object.
         */
        private ExpirationMap<String, Set<ObjectPermission>> objectPermissionsByObject =
                new ExpirationMap<String, Set<ObjectPermission>>();

        /**
         * Constructor.
         */
        public UserState()
        {
            objectPermissionsByObject.setExpiration(Duration.standardMinutes(USER_EXPIRATION_MINUTES));
        }
    }

    /**
     * Constructor.
     */
    public Cache()
    {
        // Set expiration durations
        systemPermissionsByToken.setExpiration(Duration.standardMinutes(5));
        userInformationByUserId.setExpiration(Duration.standardMinutes(USER_EXPIRATION_MINUTES));
        groupByGroupId.setExpiration(Duration.standardMinutes(USER_EXPIRATION_MINUTES));
        userStateByToken.setExpiration(Duration.standardHours(1));
        reservationRequestById.setExpiration(Duration.standardMinutes(5));
        reservationById.setExpiration(Duration.standardMinutes(5));
        executableById.setExpiration(Duration.standardSeconds(10));
    }

    /**
     * Method called each 5 minutes to clear expired items.
     */
    @Scheduled(fixedDelay = (USER_EXPIRATION_MINUTES * 60 * 1000))
    public synchronized void clearExpired()
    {
        logger.debug("Clearing expired user cache...");
        DateTime dateTimeNow = DateTime.now();
        systemPermissionsByToken.clearExpired(dateTimeNow);
        userInformationByUserId.clearExpired(dateTimeNow);
        userStateByToken.clearExpired(dateTimeNow);
        for (UserState userState : userStateByToken) {
            userState.objectPermissionsByObject.clearExpired(dateTimeNow);
        }
        reservationRequestById.clearExpired(dateTimeNow);
        reservationById.clearExpired(dateTimeNow);
        executableById.clearExpired(dateTimeNow);
    }

    /**
     * @param executableId to be removed from the {@link #executableById}
     */
    public synchronized void clearExecutable(String executableId)
    {
        executableById.remove(executableId);
    }

    /**
     * @param securityToken to be removed from the {@link #systemPermissionsByToken}
     */
    public synchronized void clearSystemPermissions(SecurityToken securityToken)
    {
        systemPermissionsByToken.remove(securityToken);
    }

    /**
     * @param securityToken
     * @param systemPermission
     * @return true whether requesting user has given {@code systemPermission},
     *         false otherwise
     */
    public synchronized boolean hasSystemPermission(SecurityToken securityToken, SystemPermission systemPermission)
    {
        Map<SystemPermission, Boolean> systemPermissions = systemPermissionsByToken.get(securityToken);
        if (systemPermissions == null) {
            systemPermissions = new HashMap<SystemPermission, Boolean>();
            systemPermissionsByToken.put(securityToken, systemPermissions);
        }
        Boolean systemPermissionResult = systemPermissions.get(systemPermission);
        if (systemPermissionResult == null) {
            systemPermissionResult = authorizationService.hasSystemPermission(securityToken, systemPermission);
            systemPermissions.put(systemPermission, systemPermissionResult);
        }
        return systemPermissionResult;
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
                throw new RuntimeException("User with id '" + userId + "' doesn't exist.");
            }
            userInformation = response.getItem(0);
            userInformationByUserId.put(userId, userInformation);
        }
        return userInformation;
    }

    /**
     * @param securityToken to be used for fetching the {@link Group}
     * @param groupId       group-id of the requested group
     * @return {@link Group} for given {@code groupId}
     */
    public synchronized Group getGroup(SecurityToken securityToken, String groupId)
    {
        Group group = groupByGroupId.get(groupId);
        if (group == null) {
            ListResponse<Group> response = authorizationService.listGroups(
                    new GroupListRequest(securityToken, groupId));
            if (response.getCount() == 0) {
                throw new RuntimeException("Group with id '" + groupId + "' doesn't exist.");
            }
            group = response.getItem(0);
            groupByGroupId.put(groupId, group);
        }
        return group;
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
     * @param objectId      of the object
     * @return set of {@link ObjectPermission} for requesting user and given {@code objectId}
     */
    public synchronized Set<ObjectPermission> getObjectPermissions(SecurityToken securityToken, String objectId)
    {
        UserState userState = getUserState(securityToken);
        Set<ObjectPermission> objectPermissions = userState.objectPermissionsByObject.get(objectId);
        if (objectPermissions == null) {
            Map<String, ObjectPermissionSet> permissionsByObject = authorizationService.listObjectPermissions(
                    new ObjectPermissionListRequest(securityToken, objectId));
            objectPermissions = new HashSet<ObjectPermission>();
            objectPermissions.addAll(permissionsByObject.get(objectId).getObjectPermissions());
            userState.objectPermissionsByObject.put(objectId, objectPermissions);
        }
        return objectPermissions;
    }

    /**
     * @param securityToken
     * @param reservationRequests
     * @return map of {@link ObjectPermission}s by reservation request identifier
     */
    public Map<String, Set<ObjectPermission>> getReservationRequestsPermissions(SecurityToken securityToken,
            Collection<ReservationRequestSummary> reservationRequests)
    {
        Map<String, Set<ObjectPermission>> permissionsByReservationRequestId = new HashMap<String, Set<ObjectPermission>>();
        Set<String> reservationRequestIds = new HashSet<String>();
        for (ReservationRequestSummary reservationRequest : reservationRequests) {
            String reservationRequestId = reservationRequest.getId();
            Set<ObjectPermission> objectPermissions =
                    getObjectPermissionsWithoutFetching(securityToken, reservationRequestId);
            if (objectPermissions != null) {
                permissionsByReservationRequestId.put(reservationRequestId, objectPermissions);
            }
            else {
                reservationRequestIds.add(reservationRequestId);
            }
        }
        if (reservationRequestIds.size() > 0) {
            permissionsByReservationRequestId.putAll(fetchObjectPermissions(securityToken, reservationRequestIds));
        }
        return permissionsByReservationRequestId;
    }

    /**
     * @param securityToken of the requesting user
     * @param objectId      of the object
     * @return set of {@link ObjectPermission} for requesting user and given {@code objectId}
     *         or null if the {@link ObjectPermission}s aren't cached
     */
    public synchronized Set<ObjectPermission> getObjectPermissionsWithoutFetching(
            SecurityToken securityToken, String objectId)
    {
        UserState userState = getUserState(securityToken);
        return userState.objectPermissionsByObject.get(objectId);
    }

    /**
     * Fetch {@link ObjectPermission}s for given {@code objectIds}.
     *
     * @param securityToken
     * @param objectIds
     * @return fetched {@link ObjectPermission}s by {@code objectIds}
     */
    public synchronized Map<String, Set<ObjectPermission>> fetchObjectPermissions(
            SecurityToken securityToken, Set<String> objectIds)
    {
        Map<String, Set<ObjectPermission>> result = new HashMap<String, Set<ObjectPermission>>();
        if (objectIds.isEmpty()) {
            return result;
        }
        UserState userState = getUserState(securityToken);
        Map<String, ObjectPermissionSet> permissionsByObject =
                authorizationService.listObjectPermissions(new ObjectPermissionListRequest(securityToken, objectIds));
        for (Map.Entry<String, ObjectPermissionSet> entry : permissionsByObject.entrySet()) {
            String objectId = entry.getKey();
            Set<ObjectPermission> objectPermissions = userState.objectPermissionsByObject.get(objectId);
            if (objectPermissions == null) {
                objectPermissions = new HashSet<ObjectPermission>();
                userState.objectPermissionsByObject.put(objectId, objectPermissions);
            }
            objectPermissions.clear();
            objectPermissions.addAll(entry.getValue().getObjectPermissions());
            result.put(objectId, objectPermissions);
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
        if (reservationRequest == null) {
            throw new CommonReportSet.ObjectNotExistsException(
                    ReservationRequestSummary.class.getSimpleName(), reservationRequestId);
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
    public synchronized Reservation getReservation(SecurityToken securityToken, String reservationId)
    {
        Reservation reservation = reservationById.get(reservationId);
        if (reservation == null) {
            reservation = reservationService.getReservation(securityToken, reservationId);
            reservationById.put(reservationId, reservation);
        }
        return reservation;
    }

    /**
     * @param securityToken
     * @param executable
     * @return reservation request id for given {@code executable}
     */
    public synchronized String getReservationRequestIdByExecutable(SecurityToken securityToken, Executable executable)
    {
        Reservation reservation = getReservation(securityToken, executable.getReservationId());
        return reservation.getReservationRequestId();
    }

    /**
     * @param securityToken
     * @param executableId
     * @return reservation request id for given {@code executableId}
     */
    public synchronized String getReservationRequestIdByExecutableId(SecurityToken securityToken, String executableId)
    {
        Executable executable = getExecutable(securityToken, executableId);
        return getReservationRequestIdByExecutable(securityToken, executable);
    }

    /**
     * @param securityToken
     * @param executableId
     * @return {@link Executable} for given {@code executableId}
     */
    public synchronized Executable getExecutable(SecurityToken securityToken, String executableId)
    {
        Executable executable = executableById.get(executableId);
        if (executable == null) {
            executable = executableService.getExecutable(securityToken, executableId);
            executableById.put(executableId, executable);
        }
        return executable;
    }
}
