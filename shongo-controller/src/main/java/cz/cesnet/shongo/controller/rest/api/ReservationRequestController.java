package cz.cesnet.shongo.controller.rest.api;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.rest.Cache;
import cz.cesnet.shongo.controller.rest.models.reservationrequest.*;
import cz.cesnet.shongo.controller.rest.models.TechnologyModel;
import cz.cesnet.shongo.controller.rest.models.room.RoomAuthorizedData;
import io.swagger.v3.oas.annotations.Operation;
import org.joda.time.Interval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.cesnet.shongo.controller.rest.auth.AuthFilter.TOKEN;

/**
 * Rest controller for reservation request endpoints.
 *
 * @author Filip Karnis
 */
@RestController
@RequestMapping("/api/v1/reservation_requests")
public class ReservationRequestController {

    private final Cache cache;
    private final ReservationService reservationService;
    private final ExecutableService executableService;

    public ReservationRequestController(
            @Autowired Cache cache,
            @Autowired ReservationService reservationService,
            @Autowired ExecutableService executableService)
    {
        this.cache = cache;
        this.reservationService = reservationService;
        this.executableService = executableService;
    }

    @Operation(summary = "Lists reservation requests.")
    @GetMapping()
    ListResponse<ReservationRequestModel> listRequests(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort", required = false,
                    defaultValue = "DATETIME") ReservationRequestListRequest.Sort sort,
            @RequestParam(value = "sort_desc", required = false, defaultValue = "true") boolean sortDescending,
            @RequestParam(value = "allocation_state", required = false) AllocationState allocationState,
            @RequestParam(value = "parentRequestId", required = false) String permanentRoomId,
            @RequestParam(value = "specification_type", required = false) Set<SpecificationType> specificationTypes,
            @RequestParam(value = "technology", required = false) TechnologyModel technology,
            @RequestParam(value = "interval_from", required = false) DateTime intervalFrom,
            @RequestParam(value = "interval_to", required = false) DateTime intervalTo,
            @RequestParam(value = "user_id", required = false) String userId,
            @RequestParam(value = "participant_user_id", required = false) String participantUserId,
            @RequestParam(value = "search", required = false) String search)
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

        if (permanentRoomId != null) {
            request.setReusedReservationRequestId(permanentRoomId);
            specificationTypes.add(SpecificationType.PERMANENT_ROOM_CAPACITY);
        }

        if (specificationTypes != null && !specificationTypes.isEmpty()) {
            if (specificationTypes.contains(SpecificationType.ADHOC_ROOM)) {
                request.addSpecificationType(ReservationRequestSummary.SpecificationType.ROOM);
            }
            if (specificationTypes.contains(SpecificationType.PERMANENT_ROOM)) {
                request.addSpecificationType(ReservationRequestSummary.SpecificationType.PERMANENT_ROOM);
            }
            if (specificationTypes.contains(SpecificationType.PERMANENT_ROOM_CAPACITY)) {
                request.addSpecificationType(ReservationRequestSummary.SpecificationType.USED_ROOM);
            }
            if (specificationTypes.contains(SpecificationType.MEETING_ROOM)) {
                request.addSpecificationType(ReservationRequestSummary.SpecificationType.RESOURCE);
            }
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
            return new ReservationRequestModel(item, permissionsByReservationRequestId, user);
        }).collect(Collectors.toList()));
        listResponse.setStart(response.getStart());
        listResponse.setCount(response.getCount());
        return listResponse;
    }

    @Operation(summary = "Returns reservation request.")
    @GetMapping("/{id:.+}")
    ReservationRequestDetailModel getRequest(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id)
    {
        ReservationRequestSummary summary = cache.getReservationRequestSummary(securityToken, id);

        List<ReservationRequestHistoryModel> history =
                reservationService.getReservationRequestHistory(securityToken, id)
                        .stream().map(ReservationRequestHistoryModel::new).collect(Collectors.toList());

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

        return new ReservationRequestDetailModel(
                summary, permissionsByReservationRequestId, ownerInformation, authorizedData, history
        );
    }

    @Operation(summary = "Accepts reservation request.")
    @PostMapping("/{id:.+}/accept")
    void acceptRequest(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id)
    {
        reservationService.confirmReservationRequest(securityToken, id, true);
    }

    @Operation(summary = "Rejects reservation request.")
    @PostMapping("/{id:.+}/reject")
    void rejectRequest(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id,
            @RequestParam(required = false) String reason)
    {
        reservationService.denyReservationRequest(securityToken, id, reason);
    }

    @Operation(summary = "Deletes reservation request.")
    @DeleteMapping("/{id:.+}")
    void deleteRequest(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id)
    {
        reservationService.deleteReservationRequest(securityToken, id);
    }

    @Operation(summary = "Reverts reservation request modifications.")
    @PostMapping("/{id:.+}/revert")
    void revertRequest(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @PathVariable String id)
    {
        reservationService.revertReservationRequest(securityToken, id);
    }
}
