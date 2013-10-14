package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebNavigation;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.BackUrl;
import cz.cesnet.shongo.client.web.support.Breadcrumb;
import cz.cesnet.shongo.client.web.support.BreadcrumbProvider;
import cz.cesnet.shongo.client.web.support.NavigationPage;
import cz.cesnet.shongo.client.web.support.editors.DateTimeEditor;
import cz.cesnet.shongo.client.web.support.editors.LocalDateEditor;
import cz.cesnet.shongo.client.web.support.editors.PeriodEditor;
import cz.cesnet.shongo.controller.api.AbstractReservationRequest;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Controller for creating/modifying reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({
        ReservationRequestUpdateController.RESERVATION_REQUEST_ATTRIBUTE,
        ReservationRequestUpdateController.PERMANENT_ROOMS_ATTRIBUTE,
        ReservationRequestUpdateController.PARTICIPANT_ATTRIBUTE
})
public class ReservationRequestUpdateController implements BreadcrumbProvider
{
    protected static final String RESERVATION_REQUEST_ATTRIBUTE = "reservationRequest";
    protected static final String PERMANENT_ROOMS_ATTRIBUTE = "permanentRooms";
    protected static final String PARTICIPANT_ATTRIBUTE = "participant";

    private static Logger logger = LoggerFactory.getLogger(ReservationRequestUpdateController.class);

    @Resource
    private Cache cache;

    @Resource
    private ReservationService reservationService;

    /**
     * {@link cz.cesnet.shongo.client.web.support.Breadcrumb} for the {@link #handleModify}
     */
    private Breadcrumb breadcrumb;

    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        binder.registerCustomEditor(DateTime.class, new DateTimeEditor());
        binder.registerCustomEditor(LocalDate.class, new LocalDateEditor());
        binder.registerCustomEditor(Period.class, new PeriodEditor());
    }

    @Override
    public Breadcrumb createBreadcrumb(NavigationPage navigationPage, String requestURI)
    {
        if (navigationPage == null) {
            return null;
        }
        if (ClientWebNavigation.RESERVATION_REQUEST_MODIFY.isNavigationPage(navigationPage)) {
            breadcrumb = new Breadcrumb(navigationPage, requestURI);
            return breadcrumb;
        }
        return new Breadcrumb(navigationPage, requestURI);
    }

    /**
     * Handle creation of a new reservation request.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_CREATE, method = {RequestMethod.GET})
    public String handleCreate(
            HttpServletRequest request,
            SecurityToken securityToken,
            @PathVariable(value = "specificationType") SpecificationType specificationType,
            @RequestParam(value = "permanentRoom", required = false) String permanentRoomId,
            @RequestParam(value = "reuse", required = false, defaultValue = "false") boolean reuse,
            Model model)
    {

        ReservationRequestModel reservationRequestModel = getReservationRequest(reuse, null, request);
        if (reservationRequestModel == null) {
            reservationRequestModel = new ReservationRequestModel(new CacheProvider(cache, securityToken));
        }
        reservationRequestModel.setSpecificationType(specificationType);
        model.addAttribute(RESERVATION_REQUEST_ATTRIBUTE, reservationRequestModel);
        if (specificationType.equals(SpecificationType.PERMANENT_ROOM_CAPACITY)) {
            if (permanentRoomId.contains(":exe:")) {
                permanentRoomId = cache.getReservationRequestIdByExecutableId(securityToken, permanentRoomId);
            }
            List<ReservationRequestSummary> permanentRooms =
                    ReservationRequestModel.getPermanentRooms(reservationService, securityToken, cache);
            model.addAttribute("permanentRooms", permanentRooms);
            reservationRequestModel.setPermanentRoomReservationRequestId(permanentRoomId, permanentRooms);
            reservationRequestModel.setTechnology(null);
        }
        return "reservationRequestCreate";
    }

    /**
     * Handle modification of an existing reservation request.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_CREATE_DUPLICATE, method = RequestMethod.GET)
    public String handleDuplicate(
            HttpServletRequest request,
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId,
            @RequestParam(value = "reuse", required = false, defaultValue = "false") boolean reuse,
            Model model)
    {
        ReservationRequestModel reservationRequestModel = getReservationRequest(reuse, null, request);
        if (reservationRequestModel == null) {
            AbstractReservationRequest reservationRequest =
                    reservationService.getReservationRequest(securityToken, reservationRequestId);
            reservationRequestModel =
                    new ReservationRequestModel(reservationRequest, new CacheProvider(cache, securityToken));
            reservationRequestModel.setId(null);
        }

        model.addAttribute(RESERVATION_REQUEST_ATTRIBUTE, reservationRequestModel);
        if (reservationRequestModel.getSpecificationType().equals(SpecificationType.PERMANENT_ROOM_CAPACITY)) {
            model.addAttribute("permanentRooms",
                    ReservationRequestModel.getPermanentRooms(reservationService, securityToken, cache));
        }
        return "reservationRequestCreate";
    }

    /**
     * Handle confirmation of creation of a new reservation request.
     */
    @RequestMapping(
            value = {ClientWebUrl.RESERVATION_REQUEST_CREATE, ClientWebUrl.RESERVATION_REQUEST_CREATE_DUPLICATE},
            method = {RequestMethod.POST})
    public String handleCreateConfirm(
            HttpServletRequest request,
            SecurityToken token,
            SessionStatus sessionStatus,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequestModel,
            BindingResult result)
    {
        if (ReservationRequestValidator.validate(reservationRequestModel, result, token, reservationService, request)) {
            AbstractReservationRequest reservationRequest = reservationRequestModel.toApi();
            String reservationRequestId = reservationService.createReservationRequest(token, reservationRequest);
            sessionStatus.setComplete();
            return "redirect:" + BackUrl.getInstance(request).getUrlNoBreadcrumb(
                    ClientWebUrl.format(ClientWebUrl.RESERVATION_REQUEST_DETAIL, reservationRequestId));
        }
        else {
            return "reservationRequestCreate";
        }
    }

    /**
     * Handle modification of an existing reservation request.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_MODIFY, method = RequestMethod.GET)
    public String handleModify(
            HttpServletRequest request,
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId,
            @RequestParam(value = "reuse", required = false, defaultValue = "false") boolean reuse,
            Model model)
    {
        ReservationRequestModel reservationRequestModel = getReservationRequest(reuse, reservationRequestId, request);
        if (reservationRequestModel == null) {
            AbstractReservationRequest reservationRequest =
                    reservationService.getReservationRequest(securityToken, reservationRequestId);
            reservationRequestModel =
                    new ReservationRequestModificationModel(reservationRequest,
                            new CacheProvider(cache, securityToken));
        }
        model.addAttribute(RESERVATION_REQUEST_ATTRIBUTE, reservationRequestModel);
        if (reservationRequestModel.getSpecificationType().equals(SpecificationType.PERMANENT_ROOM_CAPACITY)) {
            model.addAttribute("permanentRooms",
                    ReservationRequestModel.getPermanentRooms(reservationService, securityToken, cache));
        }
        if (breadcrumb != null) {
            breadcrumb.addItems(breadcrumb.getItemsCount() - 1,
                    reservationRequestModel.getBreadcrumbItems(ClientWebUrl.RESERVATION_REQUEST_DETAIL));
        }
        return "reservationRequestModify";
    }

    /**
     * Handle confirmation of modification of an existing reservation request.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_MODIFY, method = RequestMethod.POST)
    public String handleModifyConfirm(
            HttpServletRequest request,
            SecurityToken token,
            SessionStatus sessionStatus,
            @PathVariable(value = "reservationRequestId") String reservationRequestId,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequestModel,
            BindingResult result)
    {
        if (!reservationRequestId.equals(reservationRequestModel.getId())) {
            throw new IllegalArgumentException("Modification of " + reservationRequestId +
                    " was requested but attributes for " + reservationRequestModel.getId() + " was present.");
        }
        if (ReservationRequestValidator.validate(reservationRequestModel, result, token, reservationService, request)) {
            AbstractReservationRequest reservationRequest = reservationRequestModel.toApi();
            reservationRequestId = reservationService.modifyReservationRequest(token, reservationRequest);
            sessionStatus.setComplete();
            return "redirect:" + BackUrl.getInstance(request).getUrlNoBreadcrumb(
                    ClientWebUrl.format(ClientWebUrl.RESERVATION_REQUEST_DETAIL, reservationRequestId));
        }
        else {
            if (breadcrumb != null) {
                breadcrumb.addItems(breadcrumb.getItemsCount() - 1,
                        reservationRequestModel.getBreadcrumbItems(ClientWebUrl.RESERVATION_REQUEST_DETAIL));
            }
            return "reservationRequestModify";
        }
    }

    /**
     * Update reservation request attributes in the session.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_UPDATE, method = RequestMethod.POST)
    @ResponseBody
    public void handleUpdate(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequestModel)
    {
        // Reservation request has been updated in the session
    }

    /**
     * Show form for adding new participant for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_PARTICIPANT_CREATE, method = RequestMethod.GET)
    public ModelAndView handleParticipantCreate(
            SecurityToken securityToken)
    {
        ModelAndView modelAndView = new ModelAndView("participant");
        modelAndView.addObject(PARTICIPANT_ATTRIBUTE, new ParticipantModel(new CacheProvider(cache, securityToken)));
        return modelAndView;
    }

    /**
     * Store new {@code participant} to reservation request.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_PARTICIPANT_CREATE, method = RequestMethod.POST)
    public String handleParticipantCreateProcess(
            HttpServletRequest request,
            @ModelAttribute(PARTICIPANT_ATTRIBUTE) ParticipantModel participant,
            BindingResult bindingResult)
    {
        ReservationRequestModel reservationRequest = getReservationRequest(request);
        if (reservationRequest.createParticipant(participant, bindingResult)) {
            return "redirect:" + BackUrl.getInstance(request);
        }
        else {
            return "participant";
        }
    }

    /**
     * Show form for modifying existing participant for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_PARTICIPANT_MODIFY, method = RequestMethod.GET)
    public ModelAndView handleParticipantModify(
            HttpServletRequest request,
            @PathVariable("participantId") String participantId)
    {
        ReservationRequestModel reservationRequest = getReservationRequest(request);
        ParticipantModel participant = reservationRequest.getParticipant(participantId);
        ModelAndView modelAndView = new ModelAndView("participant");
        modelAndView.addObject(PARTICIPANT_ATTRIBUTE, participant);
        return modelAndView;
    }

    /**
     * Store changes for existing {@code participant} to reservation request.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_PARTICIPANT_MODIFY, method = RequestMethod.POST)
    public String handleParticipantModifyProcess(
            HttpServletRequest request,
            @PathVariable("participantId") String participantId,
            @ModelAttribute(PARTICIPANT_ATTRIBUTE) ParticipantModel participant,
            BindingResult bindingResult)
    {
        ReservationRequestModel reservationRequest = getReservationRequest(request);
        if (reservationRequest.modifyParticipant(participantId, participant, bindingResult)) {
            return "redirect:" + BackUrl.getInstance(request);
        }
        else {
            return "participant";
        }
    }

    /**
     * Delete existing {@code participant} from reservation request.
     */
    @RequestMapping(value = ClientWebUrl.RESERVATION_REQUEST_PARTICIPANT_DELETE, method = RequestMethod.GET)
    public String handleParticipantDelete(
            HttpServletRequest request,
            @PathVariable("participantId") String participantId)
    {
        ReservationRequestModel reservationRequest = getReservationRequest(request);
        reservationRequest.deleteParticipant(participantId);
        return "redirect:" + BackUrl.getInstance(request);
    }

    /**
     * Handle missing session attributes.
     */
    @ExceptionHandler(HttpSessionRequiredException.class)
    public Object handleExceptions(Exception exception)
    {
        logger.warn("Redirecting to " + ClientWebUrl.RESERVATION_REQUEST_LIST + ".", exception);
        return "redirect:" + ClientWebUrl.RESERVATION_REQUEST_LIST;
    }

    /**
     * @param reuse                specifies whether {@link ReservationRequestModel} should be tried to load from session
     * @param reservationRequestId specifies which identifier should the {@link ReservationRequestModel} have
     * @param request              whose session should be used
     * @return {@link ReservationRequestModel} from given {@code httpSession}
     */
    private ReservationRequestModel getReservationRequest(boolean reuse, String reservationRequestId,
            HttpServletRequest request)
    {
        if (!reuse) {
            return null;
        }
        Object reservationRequestAttribute = WebUtils.getSessionAttribute(request, RESERVATION_REQUEST_ATTRIBUTE);
        if (reservationRequestAttribute instanceof ReservationRequestModel) {
            ReservationRequestModel reservationRequest = (ReservationRequestModel) reservationRequestAttribute;
            if ((reservationRequestId == null && reservationRequest.getId() == null)
                    || (reservationRequestId != null && reservationRequestId.equals(reservationRequest.getId()))) {
                return reservationRequest;
            }
        }
        return null;
    }

    /**
     * @param request
     * @return {@link ReservationRequestModel} from given {@code httpSession}
     */
    private ReservationRequestModel getReservationRequest(HttpServletRequest request)
    {
        Object reservationRequestAttribute = WebUtils.getSessionAttribute(request, RESERVATION_REQUEST_ATTRIBUTE);
        if (reservationRequestAttribute instanceof ReservationRequestModel) {
            return (ReservationRequestModel) reservationRequestAttribute;
        }
        throw new IllegalStateException("Reservation request doesn't exist.");
    }
}
