package cz.cesnet.shongo.controller.rest.controllers;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.api.AbstractReservationRequest;
import cz.cesnet.shongo.controller.api.AbstractRoomExecutable;
import cz.cesnet.shongo.controller.api.AllocationState;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.rest.Cache;
import cz.cesnet.shongo.controller.rest.CacheProvider;
import cz.cesnet.shongo.controller.rest.ClientWebUrl;
import cz.cesnet.shongo.controller.rest.models.TechnologyModel;
import cz.cesnet.shongo.controller.rest.models.reservationrequest.*;
import cz.cesnet.shongo.controller.rest.models.roles.UserRoleModel;
import cz.cesnet.shongo.controller.rest.models.room.RoomAuthorizedData;
import io.swagger.v3.oas.annotations.Operation;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.cesnet.shongo.controller.rest.config.security.AuthFilter.TOKEN;

/**
 * Rest controller for reservation request endpoints.
 *
 * @author Filip Karnis
 */
@RestController
@RequestMapping(ClientWebUrl.RESERVATION_REQUESTS)
public class ReservationRequestController
{

    private final Cache cache;
    private final ReservationService reservationService;
    private final AuthorizationService authorizationService;
    private final ExecutableService executableService;

    public ReservationRequestController(
            @Autowired Cache cache,
            @Autowired ReservationService reservationService,
            @Autowired AuthorizationService authorizationService,
            @Autowired ExecutableService executableService)
    {
        this.cache = cache;
        this.reservationService = reservationService;
        this.authorizationService = authorizationService;
        this.executableService = executableService;
    }

    @Operation(summary = "Lists reservation requests.")
    @GetMapping
    ListResponse<ReservationRequestModel> listRequests(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort", required = false,
                    defaultValue = "DATETIME") ReservationRequestListRequest.Sort sort,
            @RequestParam(value = "sort_desc", required = false, defaultValue = "true") boolean sortDescending,
            @RequestParam(value = "allocation_state", required = false) AllocationState allocationState,
            @RequestParam(value = "parentRequestId", required = false) String permanentRoomId,
            @RequestParam(value = "technology", required = false) TechnologyModel technology,
            @RequestParam(value = "interval_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            DateTime intervalFrom,
            @RequestParam(value = "interval_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            DateTime intervalTo,
            @RequestParam(value = "user_id", required = false) String userId,
            @RequestParam(value = "participant_user_id", required = false) String participantUserId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "type", required = false) Set<ReservationType> reservationTypes,
            @RequestParam(value = "resource", required = false) String resourceId)
    {
        ReservationRequestListRequest request = new ReservationRequestListRequest();

        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.setSort(sort);
        request.setSortDescending(sortDescending);
        request.setAllocationState(allocationState);
        request.setParticipantUserId(participantUserId);
        request.setSearch(search);
        request.setSpecificationResourceId(resourceId);

        if (reservationTypes == null) {
            reservationTypes = new HashSet<>();
        }
        if (permanentRoomId != null) {
            request.setReusedReservationRequestId(permanentRoomId);
            reservationTypes.add(ReservationType.ROOM_CAPACITY);
        }

        if (reservationTypes.contains(ReservationType.VIRTUAL_ROOM)) {
            request.addSpecificationType(ReservationRequestSummary.SpecificationType.ROOM);
            request.addSpecificationType(ReservationRequestSummary.SpecificationType.PERMANENT_ROOM);
        }
        if (reservationTypes.contains(ReservationType.ROOM_CAPACITY)) {
            request.addSpecificationType(ReservationRequestSummary.SpecificationType.USED_ROOM);
        }
        if (reservationTypes.contains(ReservationType.PHYSICAL_RESOURCE)) {
            request.addSpecificationType(ReservationRequestSummary.SpecificationType.RESOURCE);
        }

        if (technology != null) {
            request.setSpecificationTechnologies(technology.getTechnologies());
        }

        if (intervalFrom != null || intervalTo != null) {
            if (intervalFrom == null) {
                intervalFrom = Temporal.DATETIME_INFINITY_START;
            }
            if (intervalTo == null) {
                intervalTo = Temporal.DATETIME_INFINITY_END;
            }
            if (intervalTo.isAfter(intervalFrom)) {
                request.setInterval(new Interval(intervalFrom, intervalTo));
            }
        }
        if (userId != null && UserInformation.isLocal(userId)) {
            request.setUserId(userId);
        }

        ListResponse<ReservationRequestSummary> response = reservationService.listReservationRequests(request);
        Map<String, Set<ObjectPermission>> permissionsByReservationRequestId =
                cache.getReservationRequestsPermissions(securityToken, response.getItems());

        ListResponse<ReservationRequestModel> listResponse = new ListResponse<>();
        listResponse.addAll(response.getItems().stream().map(item -> {
            UserInformation user = cache.getUserInformation(securityToken, item.getUserId());
            String resource = item.getResourceId();
            ResourceSummary resourceSummary = null;
            if (resource != null) {
                resourceSummary = cache.getResourceSummary(securityToken, resource);
            }
            return new ReservationRequestModel(item, permissionsByReservationRequestId, user, resourceSummary);
        }).collect(Collectors.toList()));
        listResponse.setStart(response.getStart());
        listResponse.setCount(response.getCount());
        return listResponse;
    }

    @Operation(summary = "Creates reservation request.")
    @PostMapping
    void createRequest(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @RequestBody ReservationRequestCreateModel request)
    {
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        request.setCacheProvider(cacheProvider);

        String resourceId = request.getResourceId();
        if (resourceId != null) {
            ResourceSummary resourceSummary = cacheProvider.getResourceSummary(resourceId);
            request.setTechnology(TechnologyModel.find(resourceSummary.getTechnologies()));
        }
        UserInformation userInformation = securityToken.getUserInformation();

        if (request.getSpecificationType() == SpecificationType.VIRTUAL_ROOM) {
            // Add default participant
            request.addRoomParticipant(userInformation, request.getDefaultOwnerParticipantRole());

            // Create VIRTUAL_ROOM
            String reservationId = reservationService.createReservationRequest(securityToken, request.toApi());

            // Add default role
            request.setId(reservationId);
            UserRoleModel userRoleModel = request.addUserRole(userInformation, ObjectRole.OWNER);
            authorizationService.createAclEntry(securityToken, userRoleModel.toApi());

            // Set request to ROOM_CAPACITY for created VIRTUAL_ROOM
            request.setSpecificationType(SpecificationType.ROOM_CAPACITY);
            request.setRoomReservationRequestId(reservationId);
            request.clearRoomParticipants();
        }

        String reservationId = reservationService.createReservationRequest(securityToken, request.toApi());
        request.setId(reservationId);

        // Create default role for the user
        if (request.getSpecificationType() != SpecificationType.ROOM_CAPACITY) {
            UserRoleModel userRoleModel = request.addUserRole(userInformation, ObjectRole.OWNER);
            authorizationService.createAclEntry(securityToken, userRoleModel.toApi());
        }
    }

    @Operation(summary = "Returns reservation request.")
    @GetMapping(ClientWebUrl.ID_SUFFIX)
    ReservationRequestDetailModel getRequest(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id)
    {
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        ReservationRequestSummary summary = cache.getReservationRequestSummary(securityToken, id);

        List<ReservationRequestHistoryModel> history =
                reservationService.getReservationRequestHistory(securityToken, id)
                        .stream()
                        .map(item -> new ReservationRequestHistoryModel(item, cacheProvider))
                        .collect(Collectors.toList());

        String roomId = cache.getExecutableId(securityToken, id);
        RoomAuthorizedData authorizedData = null;
        if (roomId != null) {
            AbstractRoomExecutable roomExecutable =
                    (AbstractRoomExecutable) executableService.getExecutable(securityToken, roomId);
            authorizedData = new RoomAuthorizedData(roomExecutable);
        }

        List<ReservationRequestSummary> requests = new ArrayList<>();
        requests.add(summary);
        Map<String, Set<ObjectPermission>> permissionsByReservationRequestId =
                cache.getReservationRequestsPermissions(securityToken, requests);

        UserInformation ownerInformation = cache.getUserInformation(securityToken, summary.getUserId());

        String resourceId = summary.getResourceId();
        ResourceSummary resourceSummary = null;
        if (resourceId != null) {
            resourceSummary = cacheProvider.getResourceSummary(resourceId);
        }

        return new ReservationRequestDetailModel(
                summary, permissionsByReservationRequestId, ownerInformation, authorizedData, history, resourceSummary
        );
    }

    @Operation(summary = "Modifies reservation request.")
    @PatchMapping(ClientWebUrl.ID_SUFFIX)
    void modifyRequest(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id,
            @RequestBody ReservationRequestCreateModel request)
    {
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);

        AbstractReservationRequest originalRequest = reservationService.getReservationRequest(securityToken, id);
        ReservationRequestCreateModel modifiedRequest =
                new ReservationRequestCreateModel(originalRequest, cacheProvider);

        if (request.getRoomName() != null) {
            modifiedRequest.setRoomName(request.getRoomName());
        }
        if (request.getDescription() != null) {
            modifiedRequest.setDescription(request.getDescription());
        }
        if (request.getSlot() != null) {
            modifiedRequest.setSlot(request.getSlot());
        }
        if (request.getPeriodicity() != null) {
            modifiedRequest.setPeriodicity(request.getPeriodicity());
        }
        if (request.getResourceId() != null) {
            ResourceSummary resourceSummary = cacheProvider.getResourceSummary(request.getResourceId());
            modifiedRequest.setTechnology(TechnologyModel.find(resourceSummary.getTechnologies()));
        }
        if (request.getAdminPin() != null) {
            modifiedRequest.setAdminPin(request.getAdminPin());
        }
        if (request.getParticipantCount() != null) {
            modifiedRequest.setParticipantCount(request.getParticipantCount());
        }
        modifiedRequest.setAllowGuests(request.isAllowGuests());
        modifiedRequest.setRoomRecorded(request.isRoomRecorded());

        reservationService.modifyReservationRequest(securityToken, modifiedRequest.toApi());
    }

    @Operation(summary = "Deletes reservation request.")
    @DeleteMapping(ClientWebUrl.ID_SUFFIX)
    void deleteRequest(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id)
    {
        reservationService.deleteReservationRequest(securityToken, id);
    }

    @Operation(summary = "Deletes multiple reservation requests.")
    @DeleteMapping
    void deleteRequests(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @RequestBody List<String> ids)
    {
        ids.forEach(id -> reservationService.deleteReservationRequest(securityToken, id));
    }

    @Operation(summary = "Accepts reservation request.")
    @PostMapping(ClientWebUrl.RESERVATION_REQUESTS_ACCEPT)
    void acceptRequest(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id)
    {
        reservationService.confirmReservationRequest(securityToken, id, true);
    }

    @Operation(summary = "Rejects reservation request.")
    @PostMapping(ClientWebUrl.RESERVATION_REQUESTS_REJECT)
    void rejectRequest(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id,
            @RequestParam(required = false) String reason)
    {
        reservationService.denyReservationRequest(securityToken, id, reason);
    }

    @Operation(summary = "Reverts reservation request modifications.")
    @PostMapping(ClientWebUrl.RESERVATION_REQUESTS_REVERT)
    void revertRequest(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id)
    {
        reservationService.revertReservationRequest(securityToken, id);
    }

    public enum ReservationType
    {
        VIRTUAL_ROOM,
        PHYSICAL_RESOURCE,
        ROOM_CAPACITY
    }
}
