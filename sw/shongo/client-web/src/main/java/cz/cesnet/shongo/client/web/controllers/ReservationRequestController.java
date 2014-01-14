package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebMessage;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.*;

/**
 * Controller for listing reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class ReservationRequestController
{
    @Resource
    private ReservationService reservationService;

    @Resource
    private Cache cache;

    @Resource
    private MessageSource messageSource;

    /**
     * Handle default reservation request view.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST, method = RequestMethod.GET)
    public String handleDefaultView()
    {
        return "forward:" + ClientWebUrl.RESERVATION_REQUEST_LIST;
    }

    /**
     * Handle list of reservation requests view.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_LIST, method = RequestMethod.GET)
    public String handleListView()
    {
        return "reservationRequestList";
    }

    /**
     * Handle data request for list of reservation requests.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_LIST_DATA, method = RequestMethod.GET)
    @ResponseBody
    public Map handleListData(
            Locale locale,
            DateTimeZone timeZone,
            SecurityToken securityToken,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "sort", required = false, defaultValue = "DATETIME") ReservationRequestListRequest.Sort sort,
            @RequestParam(value = "sort-desc", required = false, defaultValue = "true") boolean sortDescending,
            @RequestParam(value = "specification-type", required = false) Set<SpecificationType> specificationTypes,
            @RequestParam(value = "allocation-state", required = false) AllocationState allocationState,
            @RequestParam(value = "permanent-room-id", required = false) String permanentRoomId)
    {
        // List reservation requests
        ReservationRequestListRequest request = new ReservationRequestListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.setSort(sort);
        request.setSortDescending(sortDescending);
        request.setAllocationState(allocationState);
        if (permanentRoomId != null) {
            request.setReusedReservationRequestId(permanentRoomId);
            specificationTypes.add(SpecificationType.PERMANENT_ROOM_CAPACITY);
        }
        if (specificationTypes != null && specificationTypes.size() > 0) {
            if (specificationTypes.contains(SpecificationType.ADHOC_ROOM)) {
                request.addSpecificationType(ReservationRequestSummary.SpecificationType.ROOM);
            }
            if (specificationTypes.contains(SpecificationType.PERMANENT_ROOM)) {
                request.addSpecificationType(ReservationRequestSummary.SpecificationType.PERMANENT_ROOM);
            }
            if (specificationTypes.contains(SpecificationType.PERMANENT_ROOM_CAPACITY)) {
                request.addSpecificationType(ReservationRequestSummary.SpecificationType.USED_ROOM);
            }
        }
        ListResponse<ReservationRequestSummary> response = reservationService.listReservationRequests(request);

        Set<String> userIds = new HashSet<String>();
        Set<String> reusedReservationRequestIds = new HashSet<String>();
        for (ReservationRequestSummary reservationRequest : response.getItems()) {
            userIds.add(reservationRequest.getUserId());
            String reusedReservationRequestId = reservationRequest.getReusedReservationRequestId();
            if (reusedReservationRequestId != null) {
                reusedReservationRequestIds.add(reusedReservationRequestId);
            }
        }
        cache.fetchUserInformation(securityToken, userIds);
        cache.fetchReservationRequests(securityToken, reusedReservationRequestIds);

        // Get permissions for reservation requests
        Map<String, Set<ObjectPermission>> permissionsByReservationRequestId =
                cache.getReservationRequestsPermissions(securityToken, response.getItems());

        // Build response
        DateTimeFormatter formatter = DateTimeFormatter.getInstance(DateTimeFormatter.SHORT, locale, timeZone);
        List<Map<String, Object>> items = new LinkedList<Map<String, Object>>();
        for (ReservationRequestSummary reservationRequest : response.getItems()) {
            String reservationRequestId = reservationRequest.getId();
            SpecificationType specificationType = SpecificationType.fromReservationRequestSummary(reservationRequest);

            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", reservationRequestId);
            item.put("description", reservationRequest.getDescription());
            item.put("dateTime", formatter.formatDate(reservationRequest.getDateTime()));
            items.add(item);

            String reservationId = reservationRequest.getLastReservationId();
            if (reservationId != null) {
                item.put("reservationId", reservationId);
            }

            ReservationRequestState state = ReservationRequestState.fromApi(reservationRequest);
            if (state != null) {
                String lastReservationId = reservationRequest.getLastReservationId();
                item.put("state", state);
                item.put("stateMessage", state.getMessage(messageSource, locale, specificationType));
                item.put("stateHelp", state.getHelp(messageSource, locale, specificationType, lastReservationId));
            }

            Set<ObjectPermission> objectPermissions = permissionsByReservationRequestId.get(reservationRequestId);
            item.put("isWritable", objectPermissions.contains(ObjectPermission.WRITE));
            item.put("isProvidable", objectPermissions.contains(ObjectPermission.PROVIDE_RESERVATION_REQUEST));

            UserInformation user = cache.getUserInformation(securityToken, reservationRequest.getUserId());
            item.put("user", user.getFullName());

            Interval earliestSlot = reservationRequest.getEarliestSlot();
            if (earliestSlot != null) {
                item.put("earliestSlot", formatter.formatInterval(earliestSlot));
                item.put("earliestSlotMultiLine", formatter.formatIntervalMultiLine(earliestSlot));
            }
            Integer futureSlotCount = reservationRequest.getFutureSlotCount();
            if (futureSlotCount != null) {
                item.put("futureSlotCount", futureSlotCount);
            }
            item.put("isDeprecated", earliestSlot.getEnd().isBeforeNow());

            Set<Technology> technologies = reservationRequest.getSpecificationTechnologies();
            TechnologyModel technology = TechnologyModel.find(technologies);
            item.put("type", specificationType);
            item.put("typeMessage", messageSource.getMessage(
                    "views.reservationRequest.specification." + specificationType, null, locale));
            switch (specificationType) {
                case PERMANENT_ROOM:
                {
                    if (technology != null) {
                        item.put("technology", technology);
                        item.put("technologyTitle", technology.getTitle());
                    }
                    item.put("roomName", reservationRequest.getRoomName());
                    break;
                }
                case PERMANENT_ROOM_CAPACITY:
                {
                    String reusedReservationRequestId = reservationRequest.getReusedReservationRequestId();
                    item.put("roomReservationRequestId", reusedReservationRequestId);
                    item.put("roomParticipantCount", reservationRequest.getRoomParticipantCount());
                    item.put("roomParticipantCountMessage", messageSource.getMessage(
                            "views.reservationRequest.specification.roomParticipantCountMessage",
                            new Object[]{reservationRequest.getRoomParticipantCount()}, locale));

                    ReservationRequestSummary reusedReservationRequest =
                            cache.getReservationRequestSummary(securityToken, reusedReservationRequestId);
                    if (reusedReservationRequest != null) {
                        item.put("room", reusedReservationRequest.getRoomName());
                    }
                    break;
                }
                case ADHOC_ROOM:
                {
                    if (technology != null) {
                        item.put("technology", technology);
                        item.put("technologyTitle", technology.getTitle());
                    }
                    item.put("roomName", messageSource.getMessage(ClientWebMessage.ROOM_NAME_ADHOC, null, locale));
                    item.put("roomParticipantCount", reservationRequest.getRoomParticipantCount());
                    item.put("roomParticipantCountMessage", messageSource.getMessage(
                            "views.reservationRequest.specification.roomParticipantCountMessage",
                            new Object[]{reservationRequest.getRoomParticipantCount()}, locale));
                    break;
                }
            }
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("sort", sort);
        data.put("sort-desc", sortDescending);
        data.put("items", items);
        return data;
    }
}
