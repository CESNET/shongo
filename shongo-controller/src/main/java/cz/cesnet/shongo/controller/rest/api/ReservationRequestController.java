package cz.cesnet.shongo.controller.rest.api;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.rest.models.reservationrequest.SpecificationType;
import cz.cesnet.shongo.controller.rest.models.TechnologyModel;
import io.swagger.v3.oas.annotations.Operation;
import org.joda.time.Interval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.joda.time.DateTime;
import java.util.Set;

import static cz.cesnet.shongo.controller.rest.auth.AuthFilter.TOKEN;

/**
 * @author Filip Karnis
 */
@RestController
@RequestMapping("/api/v1/reservation_requests")
public class ReservationRequestController {

    private final ReservationService reservationService;

    public ReservationRequestController(@Autowired ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(summary = "Lists reservation requests")
    @GetMapping("")
    ListResponse<ReservationRequestSummary> listRequests(
            @RequestAttribute(TOKEN) SecurityToken securityToken,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort", required = false,
                    defaultValue = "DATETIME") ReservationRequestListRequest.Sort sort,
            @RequestParam(value = "sort_desc", required = false, defaultValue = "true") boolean sortDescending,
            @RequestParam(value = "specification_type", required = false) Set<SpecificationType> specificationTypes,
            @RequestParam(value = "specification_technology", required = false) TechnologyModel specificationTechnology,
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
        if (specificationTechnology != null) {
            request.setSpecificationTechnologies(specificationTechnology.getTechnologies());
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
        if (participantUserId != null) {
            request.setParticipantUserId(participantUserId);
        }
        if (search != null) {
            request.setSearch(search);
        }

        return reservationService.listReservationRequests(request);
    }
}
