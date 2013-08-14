package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebNavigation;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.NavigationPage;
import cz.cesnet.shongo.client.web.editors.DateTimeEditor;
import cz.cesnet.shongo.client.web.editors.LocalDateEditor;
import cz.cesnet.shongo.client.web.editors.PeriodEditor;
import cz.cesnet.shongo.client.web.interceptors.NavigationInterceptor;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.client.web.models.ReservationRequestValidator;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * Controller for creating/modifying reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({"reservationRequest", "permanentRooms", "confirmUrl"})
public class ReservationRequestUpdateController
{
    private static Logger logger = LoggerFactory.getLogger(ReservationRequestUpdateController.class);

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

    /**
     * Handle creation of a new reservation request.
     */
    @RequestMapping(
            value = ClientWebUrl.RESERVATION_REQUEST_CREATE,
            method = {RequestMethod.GET})
    public String handleCreate(
            SecurityToken securityToken,
            @PathVariable(value = "type") ReservationRequestModel.SpecificationType specificationType,
            @RequestParam(value = "permanentRoom", required = false) String permanentRoom,
            Model model)
    {
        ReservationRequestModel reservationRequestModel = new ReservationRequestModel();
        reservationRequestModel.setSpecificationType(specificationType);
        reservationRequestModel.setPermanentRoomReservationRequestId(permanentRoom);
        model.addAttribute("reservationRequest", reservationRequestModel);
        model.addAttribute("permanentRooms",
                ReservationRequestModel.getPermanentRooms(reservationService, securityToken, cache));
        model.addAttribute("confirmUrl", ClientWebUrl.RESERVATION_REQUEST_CREATE_CONFIRM);
        return "reservationRequestCreate";
    }

    /**
     * Handle confirmation of creation of a new reservation request.
     */
    @RequestMapping(
            value = ClientWebUrl.RESERVATION_REQUEST_CREATE_CONFIRM,
            method = {RequestMethod.POST, RequestMethod.GET})
    public String handleCreateConfirm(
            SecurityToken token,
            SessionStatus sessionStatus,
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequestModel,
            BindingResult result)
    {
        if (ReservationRequestValidator.validate(reservationRequestModel, result, token, reservationService)) {
            AbstractReservationRequest reservationRequest = reservationRequestModel.toApi();
            String reservationRequestId = reservationService.createReservationRequest(token, reservationRequest);
            sessionStatus.setComplete();
            return "redirect:" + ClientWebUrl.format(ClientWebUrl.RESERVATION_REQUEST_DETAIL, reservationRequestId);
        }
        else {
            return "reservationRequestCreate";
        }
    }

    /**
     * Handle modification of an existing reservation request.
     */
    @RequestMapping(
            value = ClientWebUrl.RESERVATION_REQUEST_MODIFY,
            method = RequestMethod.GET)
    public String handleModify(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId,
            Model model)
    {
        AbstractReservationRequest reservationRequest =
                reservationService.getReservationRequest(securityToken, reservationRequestId);
        ReservationRequestModel reservationRequestModel = new ReservationRequestModel(reservationRequest, null);
        model.addAttribute("reservationRequest", reservationRequestModel);
        if (reservationRequestModel.getSpecificationType().equals(
                ReservationRequestModel.SpecificationType.PERMANENT_ROOM_CAPACITY)) {
            model.addAttribute("permanentRooms",
                    ReservationRequestModel.getPermanentRooms(reservationService, securityToken, cache));
        }
        model.addAttribute("confirmUrl",
                ClientWebUrl.format(ClientWebUrl.RESERVATION_REQUEST_MODIFY_CONFIRM, reservationRequestId));
        return "reservationRequestModify";
    }

    /**
     * Handle confirmation of modification of an existing reservation request.
     */
    @RequestMapping(
            value = ClientWebUrl.RESERVATION_REQUEST_MODIFY_CONFIRM,
            method = {RequestMethod.POST, RequestMethod.GET})
    public String handleModifyConfirm(
            SecurityToken token,
            SessionStatus sessionStatus,
            @PathVariable(value = "reservationRequestId") String reservationRequestId,
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequestModel,
            BindingResult result)
    {
        if (!reservationRequestId.equals(reservationRequestModel.getId())) {
            throw new IllegalArgumentException("Modification of " + reservationRequestId +
                    " was requested but attributes for " + reservationRequestModel.getId() + " was present.");
        }
        if (ReservationRequestValidator.validate(reservationRequestModel, result, token, reservationService)) {
            AbstractReservationRequest reservationRequest = reservationRequestModel.toApi();
            reservationRequestId = reservationService.modifyReservationRequest(token, reservationRequest);
            sessionStatus.setComplete();
            return "redirect:" + ClientWebUrl.format(ClientWebUrl.RESERVATION_REQUEST_DETAIL, reservationRequestId);
        }
        else {
            return "reservationRequestModify";
        }
    }

    /**
     * Handle missing session attributes.
     */
    @ExceptionHandler(HttpSessionRequiredException.class)
    public Object handleExceptions(Exception exception, HttpServletRequest request)
    {
        logger.warn("Redirecting to " + ClientWebUrl.RESERVATION_REQUEST_LIST + ".", exception);
        return "redirect:" + ClientWebUrl.RESERVATION_REQUEST_LIST;
    }
}
