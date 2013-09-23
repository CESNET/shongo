package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebMessage;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.controller.Permission;
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
            @RequestParam(value = "type", required = false) Set<SpecificationType> specificationTypes,
            @RequestParam(value = "permanent-room-id", required = false) String permanentRoomId)
    {
        // List reservation requests
        ReservationRequestListRequest request = new ReservationRequestListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.setSort(sort);
        request.setSortDescending(sortDescending);
        if (permanentRoomId != null) {
            specificationTypes.add(SpecificationType.PERMANENT_ROOM_CAPACITY);
        }
        if (specificationTypes != null && specificationTypes.size() > 0) {
            if (specificationTypes.contains(SpecificationType.ADHOC_ROOM)) {
                request.addSpecificationClass(RoomSpecification.class);
            }
            if (specificationTypes.contains(SpecificationType.PERMANENT_ROOM)) {
                request.addSpecificationClass(AliasSpecification.class);
                request.addSpecificationClass(AliasSetSpecification.class);
            }
            if (specificationTypes.contains(SpecificationType.PERMANENT_ROOM_CAPACITY)) {
                request.addSpecificationClass(RoomSpecification.class);
                if (permanentRoomId != null) {
                    request.setReusedReservationRequestId(permanentRoomId);
                }
                else if (specificationTypes.size() == 1) {
                    // We want only room capacities and thus the reused reservation request must be set
                    request.setReusedReservationRequestId(ReservationRequestListRequest.FILTER_NOT_EMPTY);
                }
            }
            else {
                // We don't want room capacities and thus the reused reservation request must be not set
                request.setReusedReservationRequestId(ReservationRequestListRequest.FILTER_EMPTY);
            }
        }
        ListResponse<ReservationRequestSummary> response = reservationService.listReservationRequests(request);

        // Get permissions for reservation requests
        Map<String, Set<Permission>> permissionsByReservationRequestId = new HashMap<String, Set<Permission>>();
        Set<String> reservationRequestIds = new HashSet<String>();
        for (ReservationRequestSummary responseItem : response.getItems()) {
            String reservationRequestId = responseItem.getId();
            Set<Permission> permissions = cache.getPermissionsWithoutFetching(securityToken, reservationRequestId);
            if (permissions != null) {
                permissionsByReservationRequestId.put(reservationRequestId, permissions);
            }
            else {
                reservationRequestIds.add(reservationRequestId);
            }
        }
        if (reservationRequestIds.size() > 0) {
            permissionsByReservationRequestId.putAll(cache.fetchPermissions(securityToken, reservationRequestIds));
        }

        Set<String> reusedReservationRequestIds = new HashSet<String>();
        for (ReservationRequestSummary reservationRequest : response.getItems()) {
            String reusedReservationRequestId = reservationRequest.getReusedReservationRequestId();
            if (reusedReservationRequestId != null) {
                reusedReservationRequestIds.add(reusedReservationRequestId);
            }
        }
        cache.fetchReservationRequests(securityToken, reusedReservationRequestIds);

        // Build response
        DateTimeFormatter formatter = DateTimeFormatter.getInstance(DateTimeFormatter.SHORT, locale, timeZone);
        List<Map<String, Object>> items = new LinkedList<Map<String, Object>>();
        for (ReservationRequestSummary reservationRequest : response.getItems()) {
            String reservationRequestId = reservationRequest.getId();

            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", reservationRequestId);
            item.put("description", reservationRequest.getDescription());
            item.put("purpose", reservationRequest.getPurpose());
            item.put("dateTime", formatter.formatDate(reservationRequest.getDateTime()));
            items.add(item);

            String reservationId = reservationRequest.getLastReservationId();
            if (reservationId != null) {
                item.put("reservationId", reservationId);
            }

            ReservationRequestState state = ReservationRequestState.fromApi(reservationRequest);
            if (state != null) {
                String stateMessage = messageSource.getMessage("views.reservationRequest.state." + state, null, locale);
                item.put("state", state);
                item.put("stateMessage", stateMessage);
                item.put("stateHelp", state.getHelp(messageSource, locale, reservationRequest.getLastReservationId()));
            }

            Set<Permission> permissions = permissionsByReservationRequestId.get(reservationRequestId);
            item.put("isWritable", permissions.contains(Permission.WRITE));

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
            ReservationRequestSummary.Specification specification = reservationRequest.getSpecification();
            SpecificationType specificationType = SpecificationType.fromReservationRequestSummary(reservationRequest);
            item.put("type", specificationType);
            item.put("typeMessage", messageSource.getMessage(
                    "views.reservationRequest.specification." + specificationType, null, locale));
            switch (specificationType) {
                case PERMANENT_ROOM:
                {
                    ReservationRequestSummary.AliasSpecification alias =
                            (ReservationRequestSummary.AliasSpecification) specification;
                    if (technology != null) {
                        item.put("technology", technology.getTitle());
                    }
                    if (alias.getAliasType().equals(AliasType.ROOM_NAME)) {
                        item.put("roomName", alias.getValue());
                    }
                    break;
                }
                case PERMANENT_ROOM_CAPACITY:
                {
                    ReservationRequestSummary.RoomSpecification room =
                            (ReservationRequestSummary.RoomSpecification) specification;
                    String reusedReservationRequestId = reservationRequest.getReusedReservationRequestId();
                    item.put("roomReservationRequestId", reusedReservationRequestId);
                    item.put("roomParticipantCount", room.getParticipantCount());
                    item.put("roomParticipantCountMessage", messageSource.getMessage(
                            "views.reservationRequest.specification.roomParticipantCountMessage",
                            new Object[]{room.getParticipantCount()}, locale));

                    ReservationRequestSummary reusedReservationRequest =
                            cache.getReservationRequestSummary(securityToken, reusedReservationRequestId);
                    if (reusedReservationRequest != null) {
                        ReservationRequestSummary.AliasSpecification aliasSpecification =
                                (ReservationRequestSummary.AliasSpecification)
                                        reusedReservationRequest.getSpecification();
                        if (aliasSpecification != null && AliasType.ROOM_NAME
                                .equals(aliasSpecification.getAliasType())) {
                            item.put("room", aliasSpecification.getValue());
                        }
                        else {
                            throw new UnsupportedApiException(aliasSpecification);
                        }
                    }
                    break;
                }
                case ADHOC_ROOM:
                {
                    ReservationRequestSummary.RoomSpecification room =
                            (ReservationRequestSummary.RoomSpecification) specification;
                    if (technology != null) {
                        item.put("technology", technology.getTitle());
                    }
                    item.put("roomName", messageSource.getMessage(ClientWebMessage.ROOM_NAME_ADHOC, null, locale));
                    item.put("roomParticipantCount", room.getParticipantCount());
                    item.put("roomParticipantCountMessage", messageSource.getMessage(
                            "views.reservationRequest.specification.roomParticipantCountMessage",
                            new Object[]{room.getParticipantCount()}, locale));
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
