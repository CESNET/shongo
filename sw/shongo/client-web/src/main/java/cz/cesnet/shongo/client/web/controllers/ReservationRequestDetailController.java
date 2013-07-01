package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.UserCache;
import cz.cesnet.shongo.client.web.models.AclRecordValidator;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.client.web.models.UnsupportedApiException;
import cz.cesnet.shongo.controller.EntityType;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.*;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.*;

/**
 * Controller for managing reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@RequestMapping("/reservation-request")
@SessionAttributes({"aclRecord"})
public class ReservationRequestDetailController
{
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forStyle("MS");

    @Resource
    private ReservationService reservationService;

    @Resource
    private AuthorizationService authorizationService;

    @Resource
    private UserCache userCache;

    @Resource
    private MessageSource messageSource;

    @RequestMapping(value = "/detail/{id:.+}", method = RequestMethod.GET)
    public String getDetail(
            Locale locale,
            SecurityToken securityToken,
            @PathVariable(value = "id") String id,
            Model model)
    {
        // Get reservation request
        AbstractReservationRequest abstractReservationRequest =
                reservationService.getReservationRequest(securityToken, id);

        // Get single reservation request
        ReservationRequest reservationRequest = null;
        String parentReservationRequestId = null;
        if (abstractReservationRequest instanceof ReservationRequest) {
            reservationRequest = (ReservationRequest) abstractReservationRequest;
            parentReservationRequestId = reservationRequest.getParentReservationRequestId();
        }

        // Get history of reservation request if it has no parent reservation request
        boolean isActive = false;
        if (parentReservationRequestId == null) {
            ReservationRequestListRequest request = new ReservationRequestListRequest();
            request.setSecurityToken(securityToken);
            request.setReservationRequestId(id);
            request.setSort(ReservationRequestListRequest.Sort.DATETIME);
            request.setSortDescending(true);
            String reservationRequestId = abstractReservationRequest.getId();
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
                throw new RuntimeException("Reservation request " + reservationRequestId + " should exist in it's history.");
            }
            currentHistoryItem.put("selected", true);

            model.addAttribute("history", historyItems);
            isActive = currentHistoryItem == historyItems.get(0);
        }
        else {
            // Reservation request with parent doesn't have history so they are automatically active
            isActive = true;
        }

        // Get reservations for single reservation request
        if (reservationRequest != null && isActive) {
            List<Map<String, Object>> reservations = new LinkedList<Map<String, Object>>();

            // Add fake not allocated reservation
            AllocationState allocationState = reservationRequest.getAllocationState();
            if (!allocationState.equals(AllocationState.ALLOCATED)) {
                Map<String, Object> reservation = new HashMap<String, Object>();
                reservation.put("slot", reservationRequest.getSlot());
                reservation.put("allocationState", allocationState);
                reservation.put("allocationStateReport", getAllocationStateReport(reservationRequest));
                reservations.add(reservation);
            }

            // Add existing reservations
            List<String> reservationIds = reservationRequest.getReservationIds();
            if (reservationIds.size() > 0) {
                ReservationListRequest reservationListRequest = new ReservationListRequest();
                reservationListRequest.setSecurityToken(securityToken);
                reservationListRequest.setReservationIds(reservationIds);
                reservationListRequest.setSort(ReservationListRequest.Sort.SLOT);
                reservationListRequest.setSortDescending(true);
                ListResponse<Reservation> response = reservationService.listReservations(reservationListRequest);
                for (Reservation reservation : response) {
                    reservations.add(getReservation(reservation, locale));
                }
            }

            model.addAttribute("reservations", reservations);
        }

        ReservationRequestModel reservationRequestModel = new ReservationRequestModel(abstractReservationRequest);
        switch (reservationRequestModel.getSpecificationType()) {
            case PERMANENT_ROOM_CAPACITY:
                String permanentRoomReservationRequestId =
                        reservationRequestModel.getPermanentRoomCapacityReservationRequestId();
                if (permanentRoomReservationRequestId == null) {
                    throw new UnsupportedApiException("Room capacity should have provided permanent room.");
                }
                model.addAttribute("permanentRoomReservationRequest",
                        reservationService.getReservationRequest(securityToken, permanentRoomReservationRequestId));
                break;
        }

        model.addAttribute("parentReservationRequestId", parentReservationRequestId);
        model.addAttribute("reservationRequest", reservationRequestModel);
        model.addAttribute("isActive", isActive);
        return "reservationRequestDetail";
    }

    @RequestMapping(value = "/{reservationRequestId:.+}/children", method = RequestMethod.GET)
    @ResponseBody
    public Map getChildren(
            Locale locale,
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count)
    {
        // List reservation requests
        ChildReservationRequestListRequest request = new ChildReservationRequestListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.setReservationRequestId(reservationRequestId);
        ListResponse<ReservationRequest> response = reservationService.listChildReservationRequests(request);


        ReservationListRequest reservationListRequest = new ReservationListRequest();
        reservationListRequest.setSecurityToken(securityToken);
        for (ReservationRequest reservationRequest : response.getItems()) {
            String reservationId = reservationRequest.getLastReservationId();
            if (reservationId != null) {
                reservationListRequest.addReservationId(reservationId);
            }
        }
        Map<String, Reservation> reservationById = new HashMap<String, Reservation>();
        if (reservationListRequest.getReservationIds().size() > 0) {
            ListResponse<Reservation> reservations = reservationService.listReservations(reservationListRequest);
            for (Reservation reservation : reservations) {
                reservationById.put(reservation.getId(), reservation);
            }
        }

        // Build response
        DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER.withLocale(locale);
        List<Map<String, Object>> children = new LinkedList<Map<String, Object>>();
        for (ReservationRequest reservationRequest : response.getItems()) {
            String reservationId = reservationRequest.getLastReservationId();
            Map<String, Object> child = getReservation(reservationById.get(reservationId), locale);
            child.put("id", reservationRequest.getId());

            Interval slot = reservationRequest.getSlot();
            child.put("slot", dateTimeFormatter.print(slot.getStart()) + " - " +
                    dateTimeFormatter.print(slot.getEnd()));

            AllocationState allocationState = reservationRequest.getAllocationState();
            String allocationStateMessage =  messageSource.getMessage(
                    "views.reservationRequest.allocationState." + allocationState, null, locale);
            String allocationStateHelp =  messageSource.getMessage(
                    "views.help.reservationRequest.allocationState." + allocationState, null, locale);
            child.put("allocationState", allocationState);
            child.put("allocationStateMessage", allocationStateMessage);
            child.put("allocationStateHelp", allocationStateHelp);
            child.put("allocationStateReport", getAllocationStateReport(reservationRequest));

            Executable.State roomState = (Executable.State) child.get("roomState");
            if (roomState != null) {
                String roomStateMessage =  messageSource.getMessage(
                        "views.reservationRequest.executableState." + roomState, null, locale);
                String roomStateHelp =  messageSource.getMessage(
                        "views.help.reservationRequest.executableState." + roomState, null, locale);
                child.put("roomStateMessage", roomStateMessage);
                child.put("roomStateHelp", roomStateHelp);
            }
            children.add(child);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("items", children);
        return data;
    }

    /**
     * @param reservationRequest
     * @return allocation state report for given {@code reservationRequest}
     */
    private Object getAllocationStateReport(ReservationRequest reservationRequest)
    {
        switch (reservationRequest.getAllocationState()) {
            case ALLOCATION_FAILED:
                return reservationRequest.getAllocationStateReport();
        }
        return null;
    }

    /**
     * @param reservation
     * @return reservation attributes
     */
    private Map<String, Object> getReservation(Reservation reservation, Locale locale)
    {
        Map<String, Object> child = new HashMap<String, Object>();

        if (reservation != null) {
            // If reservation is not null it means that the reservation is allocated
            child.put("allocationState", AllocationState.ALLOCATED);

            // Get reservation date/time slot
            child.put("slot", reservation.getSlot());

            // Reservation should contain allocated room
            Executable.ResourceRoom room = (Executable.ResourceRoom) reservation.getExecutable();
            if (room != null) {
                child.put("roomId", room.getId());

                // Set room state and report
                Executable.State roomState = room.getState();
                child.put("roomState", roomState);
                switch (roomState) {
                    case STARTING_FAILED:
                    case STOPPING_FAILED:
                        child.put("roomStateReport", room.getStateReport());
                        break;
                }

                // Set room aliases
                List<Alias> aliases = room.getAliases();
                child.put("roomAliases", formatAliases(aliases, roomState));
                child.put("roomAliasesDescription", formatAliasesDescription(aliases, roomState, locale));
            }
        }

        return child;
    }

    /**
     * @param text
     * @return formatted given {@code text} to be better selectable by triple click
     */
    private String formatSelectable(String text)
    {
        return "<span style=\"float:left\">" + text + "</span>";
    }

    /**
     * @param aliases
     * @param executableState
     * @return formatted aliases
     */
    private String formatAliases(List<Alias> aliases, Executable.State executableState)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<span class=\"aliases");
        if (!executableState.isAvailable()) {
            stringBuilder.append(" not-available");
        }
        stringBuilder.append("\">");
        int index = 0;
        for (Alias alias : aliases) {
            AliasType aliasType = alias.getType();
            String aliasValue = null;
            switch (aliasType) {
                case H323_E164:
                    aliasValue = alias.getValue();
                    break;
                case ADOBE_CONNECT_URI:
                    aliasValue = alias.getValue();
                    aliasValue = aliasValue.replaceFirst("http(s)?\\://", "");
                    if (executableState.isAvailable()) {
                        StringBuilder aliasValueBuilder = new StringBuilder();
                        aliasValueBuilder.append("<a class=\"nowrap\" href=\"");
                        aliasValueBuilder.append(aliasValue);
                        aliasValueBuilder.append("\" target=\"_blank\">");
                        aliasValueBuilder.append(alias.getValue());
                        aliasValueBuilder.append("</a>");
                        aliasValue = aliasValueBuilder.toString();
                    }
                    break;
            }
            if (aliasValue == null) {
                continue;
            }
            if (index > 0) {
                stringBuilder.append(",&nbsp;");
            }
            stringBuilder.append(aliasValue);
            index++;
        }
        stringBuilder.append("</span>");
        return stringBuilder.toString();
    }

    /**
     * @param aliases
     * @param executableState
     * @param locale
     * @return formatted description of aliases
     */
    private String formatAliasesDescription(List<Alias> aliases, Executable.State executableState, Locale locale)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<table class=\"aliases");
        if (!executableState.isAvailable()) {
            stringBuilder.append(" not-available");
        }
        stringBuilder.append("\">");
        for (Alias alias : aliases) {
            AliasType aliasType = alias.getType();
            switch (aliasType) {
                case H323_E164:
                    stringBuilder.append("<tr><td class=\"label\">");
                    stringBuilder.append(messageSource.getMessage("views.room.alias.H323_E164", null, locale));
                    stringBuilder.append(":</td><td>");
                    stringBuilder.append(formatSelectable("+420" + alias.getValue()));
                    stringBuilder.append("</td></tr>");
                    stringBuilder.append("<tr><td class=\"label\">");
                    stringBuilder.append(messageSource.getMessage("views.room.alias.H323_E164_GDS", null, locale));
                    stringBuilder.append(":</td><td>");
                    stringBuilder.append(formatSelectable("(00420)" + alias.getValue()));
                    stringBuilder.append("</td></tr>");
                    break;
                case H323_URI:
                case H323_IP:
                case SIP_URI:
                case SIP_IP:
                    stringBuilder.append("<tr><td class=\"label\">");
                    stringBuilder.append(messageSource.getMessage("views.room.alias." + aliasType, null, locale));
                    stringBuilder.append(":</td><td>");
                    stringBuilder.append(formatSelectable(alias.getValue()));
                    stringBuilder.append("</td></tr>");
                    break;
                case ADOBE_CONNECT_URI:
                    stringBuilder.append("<tr><td class=\"label\">");
                    stringBuilder.append(messageSource.getMessage("views.room.alias." + aliasType, null, locale));
                    stringBuilder.append(":</td><td>");
                    if (executableState.isAvailable()) {
                        stringBuilder.append("<a class=\"nowrap\" href=\"");
                        stringBuilder.append(alias.getValue());
                        stringBuilder.append("\" target=\"_blank\">");
                        stringBuilder.append(alias.getValue());
                        stringBuilder.append("</a>");
                    }
                    else {
                        stringBuilder.append(alias.getValue());
                    }
                    stringBuilder.append("</td></tr>");
                    break;
            }
        }
        stringBuilder.append("</table>");
        if (!executableState.isAvailable()) {
            stringBuilder.append("<span class=\"aliases not-available\">");
            stringBuilder.append(messageSource.getMessage("views.room.notAvailable", null, locale));
            stringBuilder.append("</span>");
        }
        return stringBuilder.toString();
    }

    @RequestMapping(value = "/{reservationRequestId:.+}/acl", method = RequestMethod.GET)
    @ResponseBody
    public Map getAcl(
            Locale locale,
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId,
            @RequestParam(value = "start", required = false) Integer start,
            @RequestParam(value = "count", required = false) Integer count)
    {
        // List reservation requests
        AclRecordListRequest request = new AclRecordListRequest();
        request.setSecurityToken(securityToken);
        request.setStart(start);
        request.setCount(count);
        request.addEntityId(reservationRequestId);
        ListResponse<AclRecord> response = authorizationService.listAclRecords(request);

        // Build response
        List<Map<String, Object>> items = new LinkedList<Map<String, Object>>();
        for (AclRecord aclRecord : response.getItems()) {

            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", aclRecord.getId());
            item.put("user", userCache.getUserInformation(securityToken, aclRecord.getUserId()));
            String role = aclRecord.getRole().toString();
            item.put("role", messageSource.getMessage("views.aclRecord.role." + role, null, locale));
            items.add(item);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("items", items);
        return data;
    }

    private ModelAndView getAclCreate(String reservationRequestId, AclRecord aclRecord)
    {
        ModelAndView modelAndView = new ModelAndView("aclRecordCreate");
        modelAndView.addObject("aclRecord", aclRecord);
        modelAndView.addObject("entity", "views.reservationRequest");
        modelAndView.addObject("roles", EntityType.RESERVATION_REQUEST.getOrderedRoles());
        modelAndView.addObject("confirm", "views.button.create");
        modelAndView.addObject("confirmUrl", "/reservation-request/" + reservationRequestId + "/acl/create/confirmed");
        modelAndView.addObject("backUrl", "/reservation-request/detail/" + reservationRequestId);
        return modelAndView;
    }

    @RequestMapping(value = "/{reservationRequestId:.+}/acl/create", method = RequestMethod.GET)
    public ModelAndView getAclCreate(
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        AclRecord aclRecord = new AclRecord();
        aclRecord.setEntityId(reservationRequestId);
        return getAclCreate(reservationRequestId, aclRecord);
    }

    @RequestMapping(value = "/{reservationRequestId:.+}/acl/create/confirmed", method = RequestMethod.POST)
    public Object getAclCreateConfirmed(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId,
            @ModelAttribute("aclRecord") AclRecord aclRecord,
            BindingResult result)
    {
        AclRecordValidator aclRecordValidator = new AclRecordValidator();
        aclRecordValidator.validate(aclRecord, result);
        if (result.hasErrors()) {
            return getAclCreate(reservationRequestId, aclRecord);
        }
        authorizationService.createAclRecord(securityToken,
                aclRecord.getUserId(), aclRecord.getEntityId(), aclRecord.getRole());
        return "redirect:/reservation-request/detail/" + reservationRequestId;
    }

    @RequestMapping(value = "/{reservationRequestId:.+}/acl/delete/{aclRecordId}", method = RequestMethod.GET)
    public String getAclDelete(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId,
            @PathVariable(value = "aclRecordId") String aclRecordId,
            Model model)
    {
        AclRecordListRequest request = new AclRecordListRequest();
        request.setSecurityToken(securityToken);
        request.addEntityId(reservationRequestId);
        request.addRole(Role.OWNER);
        ListResponse<AclRecord> aclRecords = authorizationService.listAclRecords(request);
        if (aclRecords.getItemCount() == 1 && aclRecords.getItem(0).getId().equals(aclRecordId)) {
            model.addAttribute("title", "views.reservationRequestDetail.userRoles.cannotDeleteLastOwner.title");
            model.addAttribute("message", "views.reservationRequestDetail.userRoles.cannotDeleteLastOwner.message");
            model.addAttribute("backUrl", "/reservation-request/detail/" + reservationRequestId);
            return "message";
        }
        authorizationService.deleteAclRecord(securityToken, aclRecordId);
        return "redirect:/reservation-request/detail/" + reservationRequestId;
    }
}
