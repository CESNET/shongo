package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.editors.DateTimeEditor;
import cz.cesnet.shongo.client.web.editors.LocalDateEditor;
import cz.cesnet.shongo.client.web.editors.PeriodEditor;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.controller.Permission;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Controller for managing reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@RequestMapping("/reservation-request")
@SessionAttributes({"reservationRequest", "permanentRooms"})
public class ReservationRequestUpdateController
{
    @Resource
    private Cache cache;

    @Resource
    private ReservationService reservationService;

    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        binder.registerCustomEditor(DateTime.class, new DateTimeEditor());
        binder.registerCustomEditor(LocalDate.class, new LocalDateEditor());
        binder.registerCustomEditor(Period.class, new PeriodEditor());
    }

    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public String getCreate(
            SecurityToken securityToken,
            @RequestParam(value = "type", required = false) ReservationRequestModel.SpecificationType specificationType,
            @RequestParam(value = "permanentRoom", required = false) String permanentRoom,
            Model model)
    {
        ReservationRequestModel reservationRequest = new ReservationRequestModel();
        reservationRequest.setStart(Temporal.roundDateTimeToMinutes(DateTime.now(), 5));
        reservationRequest.setPeriodicityType(ReservationRequestModel.PeriodicityType.NONE);
        reservationRequest.setSpecificationType(specificationType);
        reservationRequest.setPermanentRoomCapacityReservationRequestId(permanentRoom);
        model.addAttribute("reservationRequest", reservationRequest);
        model.addAttribute("permanentRooms", getPermanentRooms(securityToken));
        return "reservationRequestCreate";
    }

    @RequestMapping(value = "/create/confirmed", method = {RequestMethod.POST, RequestMethod.GET})
    public String getCreateConfirmed(
            SecurityToken securityToken,
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequestModel,
            BindingResult result)
    {
        reservationRequestModel.validate(result);
        if (result.hasErrors()) {
            return "reservationRequestCreate";
        }
        switch (reservationRequestModel.getSpecificationType()) {
            case PERMANENT_ROOM:
                Object isSpecificationAvailable = reservationService.checkSpecificationAvailability(securityToken,
                        reservationRequestModel.toSpecificationApi(), reservationRequestModel.getSlot());
                if (!isSpecificationAvailable.equals(Boolean.TRUE)) {
                    result.rejectValue("permanentRoomName", "validation.field.permanentRoomNameNotAvailable");
                    return "reservationRequestCreate";
                }
                break;
            case PERMANENT_ROOM_CAPACITY:
                // Set technology from permanent room reservation request
                String permanentRoomReservationRequestId =
                        reservationRequestModel.getPermanentRoomCapacityReservationRequestId();
                AbstractReservationRequest permanentRoomReservationRequest =
                        reservationService.getReservationRequest(securityToken, permanentRoomReservationRequestId);
                ReservationRequestModel permanentRoomReservationRequestModel = new ReservationRequestModel();
                permanentRoomReservationRequestModel.fromSpecificationApi(
                        permanentRoomReservationRequest.getSpecification(), permanentRoomReservationRequestId);
                reservationRequestModel.setTechnology(permanentRoomReservationRequestModel.getTechnology());
                break;
        }
        AbstractReservationRequest reservationRequest = reservationRequestModel.toApi();
        String reservationRequestId = reservationService.createReservationRequest(securityToken, reservationRequest);
        return "redirect:/reservation-request/detail/" + reservationRequestId;
    }

    @RequestMapping(value = "/modify/{id:.+}", method = RequestMethod.GET)
    public String getModify(
            SecurityToken securityToken,
            @PathVariable(value = "id") String id,
            Model model)
    {
        AbstractReservationRequest reservationRequest = reservationService.getReservationRequest(securityToken, id);
        ReservationRequestModel reservationRequestModel = new ReservationRequestModel(reservationRequest);
        model.addAttribute("reservationRequest", reservationRequestModel);
        if (reservationRequestModel.getSpecificationType().equals(
                ReservationRequestModel.SpecificationType.PERMANENT_ROOM_CAPACITY)) {
            model.addAttribute("permanentRooms", getPermanentRooms(securityToken));
        }
        return "reservationRequestModify";
    }

    @RequestMapping(value = "/modify/confirmed", method = {RequestMethod.POST, RequestMethod.GET})
    public String getModifyConfirmed(
            SecurityToken securityToken,
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequestModel,
            BindingResult result)
    {
        reservationRequestModel.validate(result);
        if (result.hasErrors()) {
            return "reservationRequestModify";
        }
        AbstractReservationRequest reservationRequest = reservationRequestModel.toApi();
        String reservationRequestId = reservationService.modifyReservationRequest(securityToken, reservationRequest);
        return "redirect:/reservation-request/detail/" + reservationRequestId;
    }

    /**
     * @param securityToken
     * @return list of reservation requests for permanent rooms
     */
    private List<ReservationRequestSummary> getPermanentRooms(SecurityToken securityToken)
    {
        ReservationRequestListRequest request = new ReservationRequestListRequest();
        request.setSecurityToken(securityToken);
        request.addSpecificationClass(AliasSpecification.class);
        request.addSpecificationClass(AliasSetSpecification.class);
        List<ReservationRequestSummary> reservationRequests = new LinkedList<ReservationRequestSummary>();

        ListResponse<ReservationRequestSummary> response =  reservationService.listReservationRequests(request);
        if (response.getItemCount() > 0) {
            Set<String> reservationRequestIds = new HashSet<String>();
            for (ReservationRequestSummary reservationRequestSummary : response) {
                reservationRequestIds.add(reservationRequestSummary.getId());
            }
            cache.fetchPermissions(securityToken, reservationRequestIds);

            for (ReservationRequestSummary reservationRequestSummary : response) {
                if (!AllocationState.ALLOCATED.equals(reservationRequestSummary.getAllocationState())) {
                    continue;
                }
                Set<Permission> permissions = cache.getPermissions(securityToken, reservationRequestSummary.getId());
                if (!permissions.contains(Permission.PROVIDE_RESERVATION_REQUEST)) {
                    continue;
                }
                reservationRequests.add(reservationRequestSummary);
            }
        }



        return reservationRequests;
    }
}
