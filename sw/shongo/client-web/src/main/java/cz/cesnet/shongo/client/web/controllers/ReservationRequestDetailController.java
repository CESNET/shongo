package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.UserCache;
import cz.cesnet.shongo.client.web.models.AclRecordValidator;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
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
@SessionAttributes({"reservationRequest", "aclRecord"})
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
            SecurityToken securityToken,
            @PathVariable(value = "id") String id,
            Model model)
    {
        // Get reservation request
        AbstractReservationRequest abstractReservationRequest =
                reservationService.getReservationRequest(securityToken, id);

        // Get history of reservation request
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
            throw new RuntimeException("Reservation request " + reservationRequestId + "should exist in it's history.");
        }
        currentHistoryItem.put("selected", true);

        // Get reservation request as child
        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;
            String reservationId = reservationRequest.getLastReservationId();
            Reservation reservation = null;
            if (reservationId != null) {
                reservation = reservationService.getReservation(securityToken, reservationId);
            }
            model.addAttribute("childReservationRequest", getChild(reservationRequest, reservation));
        }

        model.addAttribute("reservationRequest", new ReservationRequestModel(abstractReservationRequest));
        model.addAttribute("history", historyItems);
        model.addAttribute("isActive", currentHistoryItem == historyItems.get(0));
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
            Map<String, Object> child = getChild(reservationRequest, reservationById.get(reservationId));
            Interval slot = (Interval) child.get("slot");
            child.put("slot", dateTimeFormatter.print(slot.getStart()) + " - " + dateTimeFormatter.print(slot.getEnd()));
            AllocationState allocationState = (AllocationState) child.get("allocationState");
            String allocationStateMessage =  messageSource.getMessage(
                    "views.reservationRequest.allocationState." + allocationState, null, locale);
            String allocationStateHelp =  messageSource.getMessage(
                    "views.help.reservationRequest.allocationState." + allocationState, null, locale);
            child.put("allocationStateMessage", allocationStateMessage);
            child.put("allocationStateHelp", allocationStateHelp);
            children.add(child);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("start", response.getStart());
        data.put("count", response.getCount());
        data.put("items", children);
        return data;
    }

    private Map<String, Object> getChild(ReservationRequest reservationRequest, Reservation reservation)
    {
        Map<String, Object> child = new HashMap<String, Object>();
        child.put("id", reservationRequest.getId());
        child.put("slot", reservationRequest.getSlot());

        AllocationState allocationState = reservationRequest.getAllocationState();
        child.put("allocationState", allocationState);

        String allocationStateReport = null;
        switch (allocationState) {
            case ALLOCATION_FAILED:
                allocationStateReport = reservationRequest.getAllocationStateReport();
        }
        child.put("allocationStateReport", allocationStateReport);

        if (reservation != null) {
            child.put("aliases", reservation.getExecutable().getState());
        }

        return child;
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
