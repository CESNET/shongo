package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.ExpirationSet;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.auth.UserPermission;
import cz.cesnet.shongo.client.web.models.UnsupportedApiException;
import cz.cesnet.shongo.client.web.resource.ResourcesUtilization;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.SystemPermission;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.*;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.api.rpc.ResourceService;
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
    private ResourceService resourceService;

    @Resource
    private ReservationService reservationService;

    @Resource
    private ExecutableService executableService;

    /**
     * {@link UserInformation}s by {@link SecurityToken}.
     */
    private ExpirationMap<SecurityToken, Map<UserPermission, Boolean>> userPermissionsByToken =
            new ExpirationMap<SecurityToken, Map<UserPermission, Boolean>>();

    /**
     * {@link UserInformation}s by user-ids.
     */
    private ExpirationMap<String, UserInformation> userInformationByUserId =
            new ExpirationMap<String, UserInformation>();

    /**
     * {@link Group}s by group-ids.
     */
    private ExpirationMap<String, Group> groupByGroupId =
            new ExpirationMap<String, Group>();

    /**
     * {@link UserState}s by {@link SecurityToken}.
     */
    private ExpirationMap<SecurityToken, UserState> userStateByToken = new ExpirationMap<SecurityToken, UserState>();

    /**
     * {@link ResourceSummary} by identifier.
     */
    private ExpirationMap<String, ResourceSummary> resourceById =
            new ExpirationMap<String, ResourceSummary>();

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
     * Ids of resources with public calendar by their calendarUriKey
     */
    private ExpirationMap<String,String> resourceIdsWithPublicCalendarByUriKey =
            new ExpirationMap<String,String>();

    /**
     * {@link Reservation} by identifier.
     */
    private ExpirationMap<String, Executable> executableById =
            new ExpirationMap<String, Executable>();

    /**
     * @see ResourcesUtilization
     */
    private final ExpirationMap<SecurityToken, ResourcesUtilization> resourcesUtilizationByToken =
            new ExpirationMap<SecurityToken, ResourcesUtilization>();

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
        userPermissionsByToken.setExpiration(Duration.standardMinutes(5));
        userInformationByUserId.setExpiration(Duration.standardMinutes(USER_EXPIRATION_MINUTES));
        groupByGroupId.setExpiration(Duration.standardMinutes(USER_EXPIRATION_MINUTES));
        userStateByToken.setExpiration(Duration.standardHours(1));
        resourceById.setExpiration(Duration.standardHours(1));
        reservationRequestById.setExpiration(Duration.standardMinutes(5));
        reservationById.setExpiration(Duration.standardMinutes(5));
        executableById.setExpiration(Duration.standardSeconds(10));
        resourcesUtilizationByToken.setExpiration(Duration.standardMinutes(10));
        resourceIdsWithPublicCalendarByUriKey.setExpiration(Duration.standardMinutes(10));
    }

    /**
     * Method called each 5 minutes to clear expired items.
     */
    @Scheduled(fixedDelay = (USER_EXPIRATION_MINUTES * 60 * 1000))
    public synchronized void clearExpired()
    {
        logger.debug("Clearing expired user cache...");
        DateTime dateTimeNow = DateTime.now();
        userPermissionsByToken.clearExpired(dateTimeNow);
        userInformationByUserId.clearExpired(dateTimeNow);
        userStateByToken.clearExpired(dateTimeNow);
        for (UserState userState : userStateByToken) {
            userState.objectPermissionsByObject.clearExpired(dateTimeNow);
        }
        resourceById.clearExpired(dateTimeNow);
        reservationRequestById.clearExpired(dateTimeNow);
        reservationById.clearExpired(dateTimeNow);
        executableById.clearExpired(dateTimeNow);
        resourcesUtilizationByToken.clearExpired(dateTimeNow);
        resourceIdsWithPublicCalendarByUriKey.clearExpired(dateTimeNow);
    }

    /**
     * @param executableId to be removed from the {@link #executableById}
     */
    public synchronized void clearExecutable(String executableId)
    {
        executableById.remove(executableId);
    }

    /**
     * @param securityToken to be removed from the {@link #userPermissionsByToken}
     */
    public synchronized void clearUserPermissions(SecurityToken securityToken)
    {
        userPermissionsByToken.remove(securityToken);
    }

    /**
     * @param securityToken
     * @param systemPermission
     * @return true whether requesting user has given {@code systemPermission},
     *         false otherwise
     */
    public boolean hasSystemPermission(SecurityToken securityToken, SystemPermission systemPermission)
    {
        return authorizationService.hasSystemPermission(securityToken, systemPermission);
    }

    public boolean hasUserPermission(SecurityToken securityToken, UserPermission userPermission)
    {
        Map<UserPermission, Boolean> userPermissions;
        synchronized (this) {
            userPermissions = userPermissionsByToken.get(securityToken);
            if (userPermissions == null) {
                userPermissions = new HashMap<UserPermission, Boolean>();
                userPermissionsByToken.put(securityToken, userPermissions);
            }
        }
        synchronized (userPermissions) {
            Boolean userPermissionResult = userPermissions.get(userPermission);
            if (userPermissionResult == null) {
                switch (userPermission) {
                    case ADMINISTRATION:
                        // Handle as SystemPermission.ADMINISTRATION
                        userPermissionResult = hasSystemPermission(securityToken, SystemPermission.ADMINISTRATION);
                        break;
                    case OPERATOR:
                        // Handle as SystemPermission.OPERATOR
                        userPermissionResult = hasSystemPermission(securityToken, SystemPermission.OPERATOR);
                        break;
                    case RESERVATION:
                        // Handle as SystemPermission.RESERVATION
                        userPermissionResult = hasSystemPermission(securityToken, SystemPermission.RESERVATION);
                        break;
                    case RESOURCE_MANAGEMENT:
                        // User has access to resource management if he is OPERATOR
                        if (hasUserPermission(securityToken, UserPermission.OPERATOR)) {
                            userPermissionResult = true;
                        }
                        // Or if he can see some resources
                        else {
                            ListResponse<ResourceSummary> resources =
                                    resourceService.listResources(new ResourceListRequest(securityToken));
                            userPermissionResult = (resources.getItemCount() > 0);
                        }
                        break;
                }
                userPermissions.put(userPermission, userPermissionResult);
            }
            return userPermissionResult;
        }
    }

    /**
     * @param securityToken to be used for fetching the {@link UserInformation}
     * @param userId        user-id of the requested user
     * @return {@link UserInformation} for given {@code userId}
     */
    public synchronized UserInformation getUserInformation(SecurityToken securityToken, String userId)
    {
        if (userId == null) {
            return null;
        }
        UserInformation userInformation = userInformationByUserId.get(userId);
        if (userInformation == null) {
            try {
                ListResponse<UserInformation> response = authorizationService.listUsers(
                        new UserListRequest(securityToken, userId));
                if (response.getCount() == 0) {
                    throw new ControllerReportSet.UserNotExistsException(userId);
                }
                userInformation = response.getItem(0);
            }
            catch (ControllerReportSet.UserNotExistsException exception) {
                logger.warn("User with id '" + userId + "' doesn't exist.", exception);
                userInformation = createNotExistingUserInformation(userId);
            }
            userInformationByUserId.put(userId, userInformation);
        }
        return userInformation;
    }

    /**
     * @param securityToken to be used for fetching the {@link UserInformation}s
     * @param userIds       user-ids of the requested users
     */
    public synchronized void fetchUserInformation(SecurityToken securityToken, Collection<String> userIds)
    {
        Set<String> missingUserIds = null;
        for (String userId : userIds) {
            if (!userInformationByUserId.contains(userId)) {
                if (missingUserIds == null) {
                    missingUserIds = new HashSet<String>();
                }
                missingUserIds.add(userId);
            }
        }
        if (missingUserIds != null) {
            while (missingUserIds.size() > 0) {
                try {
                    ListResponse<UserInformation> response = authorizationService.listUsers(
                            new UserListRequest(securityToken, missingUserIds));
                    for (UserInformation userInformation : response.getItems()) {
                        String userId = userInformation.getUserId();
                        userInformationByUserId.put(userId, userInformation);
                        missingUserIds.remove(userId);
                    }
                    if (missingUserIds.size() > 0) {
                        throw new ControllerReportSet.UserNotExistsException(missingUserIds.iterator().next());
                    }
                }
                catch (ControllerReportSet.UserNotExistsException exception) {
                    String userId = exception.getUser();
                    logger.warn("User with id '" + userId + "' doesn't exist.", exception);
                    UserInformation userInformation = createNotExistingUserInformation(userId);
                    userInformationByUserId.put(userId, userInformation);
                    missingUserIds.remove(userId);
                    continue;
                }
            }
        }
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
     * @param securityToken to be used for fetching the {@link ResourceSummary}s
     * @param resourceIds   resource-ids to be fetched
     */
    public synchronized void fetchResourceSummaries(SecurityToken securityToken, Collection<String> resourceIds)
    {
        Set<String> missingResourceIds = null;
        for (String resourceId : resourceIds) {
            if (!resourceById.contains(resourceId)) {
                if (missingResourceIds == null) {
                    missingResourceIds = new HashSet<String>();
                }
                missingResourceIds.add(resourceId);
            }
        }
        if (missingResourceIds != null) {
            ResourceListRequest request = new ResourceListRequest();
            request.setSecurityToken(securityToken);
            for (String resourceId : request.getResourceIds()) {
                request.addResourceId(resourceId);
            }
            ListResponse<ResourceSummary> response = resourceService.listResources(request);
            for (ResourceSummary resource : response.getItems()) {
                String resourceId = resource.getId();
                resourceById.put(resourceId, resource);
                missingResourceIds.remove(resourceId);
            }
            if (missingResourceIds.size() > 0) {
                throw new CommonReportSet.ObjectNotExistsException(ResourceSummary.class.getSimpleName(),
                        missingResourceIds.iterator().next());
            }
        }
    }

    /**
     * @param securityToken
     * @param resourceId
     * @return {@link ResourceSummary} for given {@code resourceId}
     */
    public ResourceSummary getResourceSummary(SecurityToken securityToken, String resourceId)
    {
        ResourceSummary resourceSummary = resourceById.get(resourceId);
        if (resourceSummary == null) {
            ResourceListRequest request = new ResourceListRequest();
            request.setSecurityToken(securityToken);
            request.addResourceId(resourceId);
            ListResponse<ResourceSummary> response = resourceService.listResources(request);
            if (response.getItemCount() == 1) {
                resourceSummary = response.getItem(0);
                resourceById.put(resourceSummary.getId(), resourceSummary);
            }
        }
        return resourceSummary;
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
                if (reservationRequest.isAllowCache()) {
                    reservationRequestById.put(reservationRequest.getId(), reservationRequest);
                }
            }
        }
    }

    /**
     * Retrieve {@link ReservationRequestSummary} from {@link Cache} or from {@link #reservationService}
     * if it doesn't exist in the {@link Cache}.
     *
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
     * Similar as {@link #getReservationRequestSummary} but the {@link ReservationRequestSummary} is loaded from
     * the {@link #reservationService} also when it is in {@link AllocationState#NOT_ALLOCATED} state (to update it).
     *
     * @param securityToken
     * @param reservationRequestId
     * @return {@link ReservationRequestSummary} for given {@code reservationRequestId}
     */
    public synchronized ReservationRequestSummary getAllocatedReservationRequestSummary(SecurityToken securityToken,
            String reservationRequestId)
    {
        ReservationRequestSummary reservationRequest = reservationRequestById.get(reservationRequestId);
        if (reservationRequest == null || reservationRequest.getAllocatedReservationId() == null) {
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
            if (reservationRequest.isAllowCache()) {
                reservationRequestById.put(reservationRequest.getId(), reservationRequest);
            }
            return reservationRequest;
        }
        throw new ObjectInaccessibleException(reservationRequestId);
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
     * @param objectId
     * @return reservation request id for given {@code objectId}
     */
    public synchronized String getReservationRequestId(SecurityToken securityToken, String objectId)
    {
        if (objectId.contains(":req:")) {
            return objectId;
        }
        else if (objectId.contains(":rsv:")) {
            Reservation reservation = getReservation(securityToken, objectId);
            return reservation.getReservationRequestId();
        }
        else if (objectId.contains(":exe:")) {
            Executable executable = getExecutable(securityToken, objectId);
            return getReservationRequestIdByExecutable(securityToken, executable);
        }
        else {
            throw new TodoImplementException(objectId);
        }
    }

    /**
     * @param securityToken
     * @param objectId
     * @return executable id for given {@code objectId}
     */
    public String getExecutableId(SecurityToken securityToken, String objectId)
    {
        if (objectId.contains(":exe:")) {
            return objectId;
        }
        else {
            if (objectId.contains(":req:")) {
                ReservationRequestSummary request = getAllocatedReservationRequestSummary(securityToken, objectId);
                String reservationId = request.getAllocatedReservationId();
                if (reservationId == null) {
                    throw new TodoImplementException("Reservation doesn't exist.");
                }
                objectId = reservationId;
            }
            if (objectId.contains(":rsv:")) {
                Reservation reservation = getReservation(securityToken, objectId);
                Executable executable = reservation.getExecutable();
                if (executable == null) {
                    throw new UnsupportedApiException("Reservation " + objectId + " doesn't have executable.");
                }
                return executable.getId();
            }
            else {
                throw new TodoImplementException(objectId);
            }
        }
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

    /**
     * @param securityToken for which the {@link ResourcesUtilization} shall be returned
     * @param forceRefresh specifies whether a fresh version should be returned
     * @return {@link ResourcesUtilization} for given {@code securityToken}
     */
    public ResourcesUtilization getResourcesUtilization(SecurityToken securityToken, boolean forceRefresh)
    {
        synchronized (resourcesUtilizationByToken) {
            ResourcesUtilization resourcesUtilization = resourcesUtilizationByToken.get(securityToken);
            if (resourcesUtilization == null || forceRefresh) {
                resourcesUtilization = new ResourcesUtilization(securityToken, resourceService, reservationService);
                resourcesUtilizationByToken.put(securityToken, resourcesUtilization);
            }
            return resourcesUtilization;
        }
    }

    /**
     * Returns resource's ID for given uriKey.
     * Accessible resource IDs are cached.
     *
     * @param uriKey
     * @return resource's reservations iCalendar text for export
     */
    public String getResourceIdWithUriKey(String uriKey)
    {

        resourceIdsWithPublicCalendarByUriKey.clearExpired(DateTime.now());
        //Check if resource really exists
        if (resourceIdsWithPublicCalendarByUriKey.size() == 0) {
            for (ResourceSummary resourceSummary: resourceService.getResourceIdsWithPublicCalendar()) {
                resourceIdsWithPublicCalendarByUriKey.put(resourceSummary.getCalendarUriKey(),resourceSummary.getId());
            }
        }
        if (!resourceIdsWithPublicCalendarByUriKey.contains(uriKey)) {
            return null;
        }
        return resourceIdsWithPublicCalendarByUriKey.get(uriKey);
    }

    /**
     * @param userId
     * @return {@link UserInformation} for not existing user with given {@code userId}
     */
    private static UserInformation createNotExistingUserInformation(String userId)
    {
        UserInformation userInformation = new UserInformation();
        userInformation.setUserId(userId);
        userInformation.setFirstName("Non-Existent-User");
        userInformation.setLastName("(" + userId + ")");
        return userInformation;
    }
}
