package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.UserCache;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.controller.Permission;
import cz.cesnet.shongo.controller.api.ReservationRequestType;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationListRequest;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/**
 * Controller for managing reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@RequestMapping("/reservation-request")
public class ReservationRequestController
{
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forStyle("M-");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forStyle("MS");

    @Resource
    private ReservationService reservationService;

    @Resource
    private UserCache userCache;

    @Resource
    private MessageSource messageSource;

    @RequestMapping(value = {"", "/list"}, method = RequestMethod.GET)
    public String getList()
    {
        return "reservationRequestList";
    }

    @RequestMapping(value = "/detail/{id:.+}", method = RequestMethod.GET)
    public String getDetail(
            SecurityToken securityToken,
            @PathVariable(value = "id") String id,
            Model model)
    {
        // Get reservation request
        AbstractReservationRequest reservationRequest =
                reservationService.getReservationRequest(securityToken, id);

        // Get history of reservation request
        ReservationRequestListRequest request = new ReservationRequestListRequest();
        request.setSecurityToken(securityToken);
        request.setReservationRequestId(id);
        request.setSort(ReservationRequestListRequest.Sort.DATETIME);
        request.setSortDescending(true);

        String reservationRequestId = reservationRequest.getId();
        Map<String, Object> currentHistoryItem = null;
        List<Map<String, Object>> historyItems = new LinkedList<Map<String, Object>>();
        for (ReservationRequestSummary historyItem : reservationService.listReservationRequests(request)) {
            Map<String, Object> item = new HashMap<String, Object>();
            String historyItemId = historyItem.getId();
            item.put("id", historyItemId);
            item.put("dateTime", historyItem.getDateTime());
            UserInformation user = userCache.getUserInformation(securityToken, historyItem.getUserId());
            item.put("user", user.getFullName());
            item.put("type", historyItem.getType());
            if ( historyItemId.equals(reservationRequestId)) {
                currentHistoryItem = item;
            }
            historyItems.add(item);
        }
        if (currentHistoryItem == null) {
            throw new RuntimeException("Reservation request " + reservationRequestId + "should exist in it's history.");
        }
        currentHistoryItem.put("selected", true);

        model.addAttribute("reservationRequest", new ReservationRequestModel(reservationRequest));
        model.addAttribute("history", historyItems);
        model.addAttribute("isWritable", currentHistoryItem == historyItems.get(0));
        return "reservationRequestDetail";
    }

    @RequestMapping(value = "/delete/{id:.+}", method = RequestMethod.GET)
    public String getDelete(
            SecurityToken securityToken,
            @PathVariable(value = "id") String reservationRequestId, Model model)
    {
        // Get reservation request
        AbstractReservationRequest reservationRequest =
                reservationService.getReservationRequest(securityToken, reservationRequestId);

        // List allocated reservations
        ReservationListRequest reservationListRequest = new ReservationListRequest();
        reservationListRequest.setSecurityToken(securityToken);
        reservationListRequest.setReservationRequestId(reservationRequestId);
        ListResponse<Reservation> reservations = reservationService.listReservations(reservationListRequest);
        if (reservations.getItemCount() > 0) {
            // List reservation requests which has provided any of allocated reservations
            ReservationRequestListRequest reservationRequestListRequest = new ReservationRequestListRequest();
            reservationRequestListRequest.setSecurityToken(securityToken);
            for (Reservation reservation : reservations.getItems()) {
                reservationRequestListRequest.addProvidedReservationId(reservation.getId());
            }
            ListResponse<ReservationRequestSummary> reservationRequests =
                    reservationService.listReservationRequests(reservationRequestListRequest);
            model.addAttribute("dependencies", reservationRequests.getItems());
        }

        model.addAttribute("reservationRequest", reservationRequest);
        return "reservationRequestDelete";
    }

    @RequestMapping(value = "/delete/confirmed", method = RequestMethod.POST)
    public String getDeleteConfirmed(
            SecurityToken securityToken,
            @RequestParam(value = "id") String reservationRequestId)
    {
        reservationService.deleteReservationRequest(securityToken, reservationRequestId);
        return "redirect:/reservation-request";
    }

    @RequestMapping(value = "/data", method = RequestMethod.GET)
    @ResponseBody
    public Map getDataList(
            Locale locale,
            SecurityToken securityToken,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "type", required = false) ReservationRequestModel.SpecificationType specificationType)
    {
        // List reservation requests
        ReservationRequestListRequest request = new ReservationRequestListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.setSort(ReservationRequestListRequest.Sort.DATETIME);
        request.setSortDescending(true);
        if (specificationType != null) {
            switch (specificationType) {
                case ALIAS:
                    request.addSpecificationClass(AliasSpecification.class);
                    request.addSpecificationClass(AliasSetSpecification.class);
                    break;
                case ROOM:
                    request.addSpecificationClass(RoomSpecification.class);
                    break;
            }
        }
        ListResponse<ReservationRequestSummary> response = reservationService.listReservationRequests(request);

        // Get permissions for reservation requests
        Map<String, Set<Permission>> permissionsByReservationRequestId = new HashMap<String, Set<Permission>>();
        Set<String> reservationRequestIds = new HashSet<String>();
        for (ReservationRequestSummary responseItem : response.getItems()) {
            String reservationRequestId = responseItem.getId();
            Set<Permission> permissions = userCache.getPermissionsWithoutFetching(securityToken, reservationRequestId);
            if ( permissions != null ) {
                permissionsByReservationRequestId.put(reservationRequestId, permissions);
            }
            else {
                reservationRequestIds.add(reservationRequestId);
            }
        }
        if (reservationRequestIds.size() > 0) {
            permissionsByReservationRequestId.putAll(userCache.fetchPermissions(securityToken, reservationRequestIds));
        }

        // Build response
        DateTimeFormatter dateFormatter = DATE_FORMATTER.withLocale(locale);
        DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.withLocale(locale);
        List<Map<String, Object>> items = new LinkedList<Map<String, Object>>();
        for (ReservationRequestSummary reservationRequest : response.getItems()) {
            String reservationRequestId = reservationRequest.getId();

            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", reservationRequestId);
            item.put("description", reservationRequest.getDescription());
            item.put("purpose", reservationRequest.getPurpose());
            item.put("dateTime", dateFormatter.print(reservationRequest.getDateTime()));
            items.add(item);

            Set<Permission> permissions = permissionsByReservationRequestId.get(reservationRequestId);
            item.put("writable", permissions.contains(Permission.WRITE));

            UserInformation user = userCache.getUserInformation(securityToken, reservationRequest.getUserId());
            item.put("user", user.getFullName());

            Interval earliestSlot = reservationRequest.getEarliestSlot();
            if (earliestSlot != null) {
                item.put("earliestSlotStart", dateTimeFormatter.print(earliestSlot.getStart()));
                item.put("earliestSlotEnd", dateTimeFormatter.print(earliestSlot.getEnd()));
            }

            Set<Technology> technologies = reservationRequest.getTechnologies();
            ReservationRequestModel.Technology technology = ReservationRequestModel.Technology.find(technologies);
            if (technology != null) {
                item.put("technology", technology.getTitle());
            }

            ReservationRequestSummary.Specification specification = reservationRequest.getSpecification();
            if (specification instanceof ReservationRequestSummary.RoomSpecification) {
                ReservationRequestSummary.RoomSpecification roomType = (ReservationRequestSummary.RoomSpecification) specification;
                item.put("type", messageSource.getMessage("views.reservationRequest.specification.room", null, locale));
                item.put("participantCount", roomType.getParticipantCount());
            }
            else if (specification instanceof ReservationRequestSummary.AliasSpecification) {
                ReservationRequestSummary.AliasSpecification aliasType = (ReservationRequestSummary.AliasSpecification) specification;
                item.put("type", messageSource.getMessage("views.reservationRequest.specification.alias", null, locale));
                if (aliasType.getAliasType().equals(AliasType.ROOM_NAME)) {
                    item.put("roomName", aliasType.getValue());
                }
            }
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("items", items);
        return data;
    }
}
