package cz.cesnet.shongo.client.web.controllers;

import com.google.common.base.Strings;
import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.client.web.*;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.BackUrl;
import cz.cesnet.shongo.client.web.support.editors.DateTimeEditor;
import cz.cesnet.shongo.client.web.support.editors.LocalDateEditor;
import cz.cesnet.shongo.controller.api.AbstractReservationRequest;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Controller for creating a new capacity for permanent room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({
        WizardParticipantsController.RESERVATION_REQUEST_ATTRIBUTE,
        WizardParticipantsController.PARTICIPANT_ATTRIBUTE,
        "permanentRooms"})
public class WizardPermanentRoomCapacityController extends WizardParticipantsController
{
    private static Logger logger = LoggerFactory.getLogger(WizardPermanentRoomCapacityController.class);

    @Resource
    private ReservationService reservationService;

    @Resource
    private AuthorizationService authorizationService;

    @Resource
    private Cache cache;

    private static enum Page
    {
        ATTRIBUTES,
        PARTICIPANTS,
        CONFIRM,
    }

    @Override
    protected void initWizardPages(WizardView wizardView, Object currentWizardPageId)
    {
        ReservationRequestModel reservationRequest =
                (ReservationRequestModel) WebUtils.getSessionAttribute(request, RESERVATION_REQUEST_ATTRIBUTE);

        if (reservationRequest != null && reservationRequest instanceof ReservationRequestModificationModel) {
            wizardView.addPage(new WizardPage(
                    Page.ATTRIBUTES,
                    ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY,
                    "views.wizard.page.room.capacity.modify"));
        }
        else {
            wizardView.addPage(new WizardPage(
                    Page.ATTRIBUTES,
                    ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY,
                    "views.wizard.page.room.capacity.create"));
        }
        wizardView.addPage(new WizardPage(
                    Page.PARTICIPANTS,
                    ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANTS,
                    "views.wizard.page.room.participants"));
        wizardView.addPage(new WizardPage(
                Page.CONFIRM,
                ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_CONFIRM,
                "views.wizard.page.room.confirm"));
    }

    /**
     * Initialize model editors for additional types.
     *
     * @param binder to be initialized
     */
    @InitBinder
    public void initBinder(WebDataBinder binder, DateTimeZone timeZone)
    {
        binder.registerCustomEditor(DateTime.class, new DateTimeEditor(timeZone));
        binder.registerCustomEditor(LocalDate.class, new LocalDateEditor());
    }

    /**
     * Handle duplication of an existing reservation request.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_DUPLICATE, method = RequestMethod.GET)
    public String handleDuplicate(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        AbstractReservationRequest reservationRequest =
                reservationService.getReservationRequest(securityToken, reservationRequestId);
        ReservationRequestModel reservationRequestModel =
                new ReservationRequestModel(reservationRequest, new CacheProvider(cache, securityToken));
        reservationRequestModel.setId(null);
        reservationRequestModel.setStart(DateTime.now());
        WebUtils.setSessionAttribute(request, RESERVATION_REQUEST_ATTRIBUTE, reservationRequestModel);
        WebUtils.setSessionAttribute(request, "permanentRooms",
                ReservationRequestModel.getPermanentRooms(reservationService, securityToken, cache));
        return "redirect:" + BackUrl.getInstance(request).applyToUrl(ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY);
    }

    /**
     * Modify existing virtual room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_MODIFY, method = RequestMethod.GET)
    public String handleModify(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        AbstractReservationRequest reservationRequest =
                reservationService.getReservationRequest(securityToken, reservationRequestId);
        ReservationRequestModel reservationRequestModel = new ReservationRequestModificationModel(
                reservationRequest, new CacheProvider(cache, securityToken), authorizationService);
        WebUtils.setSessionAttribute(request, RESERVATION_REQUEST_ATTRIBUTE, reservationRequestModel);
        return "redirect:" + BackUrl.getInstance(request).applyToUrl(ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY);
    }

    /**
     * Book capacity for existing permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY, method = RequestMethod.GET)
    public ModelAndView handleAttributes(
            SecurityToken securityToken,
            UserSession userSession,
            HttpSession httpSession,
            @RequestParam(value = "permanentRoom", required = false) String permanentRoomId,
            @RequestParam(value = "force", required = false) boolean force)
    {
        WizardView wizardView = getCreatePermanentRoomCapacityView();

        // Add permanent rooms
        List<ReservationRequestSummary> permanentRooms =
                ReservationRequestModel.getPermanentRooms(reservationService, securityToken, cache);
        wizardView.addObject("permanentRooms", permanentRooms);

        // Add reservation request model
        ReservationRequestModel reservationRequest =
                (ReservationRequestModel) httpSession.getAttribute(RESERVATION_REQUEST_ATTRIBUTE);
        if (reservationRequest == null || force) {
            reservationRequest = new ReservationRequestModel(
                    new CacheProvider(cache, securityToken), userSession.getUserSettings());
            wizardView.addObject(RESERVATION_REQUEST_ATTRIBUTE, reservationRequest);
        }
        reservationRequest.setTechnology(null);
        reservationRequest.setSpecificationType(SpecificationType.PERMANENT_ROOM_CAPACITY);
        if (permanentRoomId != null) {
            if (permanentRoomId.contains(":exe:")) {
                permanentRoomId = cache.getReservationRequestIdByExecutableId(securityToken, permanentRoomId);
            }
            reservationRequest.setPermanentRoomReservationRequestId(permanentRoomId, permanentRooms);
        }
        else if (reservationRequest.getPermanentRoomReservationRequestId() == null) {
            throw new IllegalStateException("Permanent room capacity must be not null.");
        }

        return wizardView;
    }

    /**
     * Handle validation of attributes..
     *
     * @param reservationRequest to be validated
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY, method = {RequestMethod.POST})
    public Object handleAttributesProcess(
            UserSession userSession,
            SecurityToken securityToken,
            SessionStatus sessionStatus,
            @RequestParam(value = "finish", required = false) boolean finish,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest,
            BindingResult bindingResult)
    {
        ReservationRequestValidator validator = new ReservationRequestValidator(securityToken, reservationService,
                userSession.getLocale(), userSession.getTimeZone());
        validator.validate(reservationRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            return getCreatePermanentRoomCapacityView();
        }
        reservationRequest.loadPermanentRoom(new CacheProvider(cache, securityToken));
        if (reservationRequest.getRoomParticipants().size() == 0) {
            ParticipantRole participantRole = reservationRequest.getDefaultOwnerParticipantRole();
            if (!reservationRequest.hasUserParticipant(securityToken.getUserId(), participantRole)) {
                reservationRequest.addRoomParticipant(securityToken.getUserInformation(), participantRole);
            }
        }
        if (finish) {
            return handleConfirmed(securityToken, sessionStatus, reservationRequest);
        }
        else {
            return "redirect:" + ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANTS;
        }
    }

    /**
     * Manage participants for permanent room capacity.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANTS, method = RequestMethod.GET)
    public ModelAndView handleParticipants(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequestModel)
    {
        WizardView wizardView = getWizardView(
                Page.PARTICIPANTS, "wizardRoomParticipants.jsp");
        wizardView.addObject("createUrl", ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANT_CREATE);
        wizardView.addObject("modifyUrl", ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANT_MODIFY);
        wizardView.addObject("deleteUrl", ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANT_DELETE);
        wizardView.setNextPageUrl(WizardController.SUBMIT_RESERVATION_REQUEST);
        wizardView.addAction(WizardController.SUBMIT_RESERVATION_REQUEST_FINISH,
                "views.button.finish", WizardView.ActionPosition.RIGHT);
        return wizardView;
    }

    /**
     * Process participants form.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANTS, method = RequestMethod.POST)
    public Object handleParticipantsProcess(
            @RequestParam(value = "finish", required = false) boolean finish,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequestModel,
            BindingResult bindingResult)
    {
        if (!ReservationRequestValidator.validateParticipants(reservationRequestModel, bindingResult)) {
            return handleParticipants(reservationRequestModel);
        }
        if (finish) {
            return "redirect:" + ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_FINISH;
        }
        else {
            return "redirect:" + ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_CONFIRM;
        }
    }

    /**
     * Show form for adding new participant for permanent room capacity.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANT_CREATE, method = RequestMethod.GET)
    public ModelAndView handleParticipantCreate(
            SecurityToken securityToken,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        return handleParticipantCreate(Page.PARTICIPANTS, reservationRequest, securityToken);
    }

    /**
     * Store new {@code participant} to given {@code reservationRequest}.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANT_CREATE, method = RequestMethod.POST)
    public Object handleParticipantCreateProcess(
            HttpSession httpSession,
            @ModelAttribute(PARTICIPANT_ATTRIBUTE) ParticipantModel participant,
            BindingResult bindingResult)
    {
        ReservationRequestModel reservationRequest = getReservationRequest(httpSession);
        if (reservationRequest.createParticipant(participant, bindingResult)) {
            return "redirect:" + ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANTS;
        }
        else {
            return handleParticipantView(Page.PARTICIPANTS, reservationRequest, participant);
        }
    }

    /**
     * Show form for modifying existing permanent room capacity.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANT_MODIFY, method = RequestMethod.GET)
    public ModelAndView handleParticipantModify(
            @PathVariable("participantId") String participantId,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        return handleParticipantModify(Page.PARTICIPANTS, reservationRequest, participantId);
    }

    /**
     * Store changes for existing {@code participant} to given {@code reservationRequest}.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANT_MODIFY, method = RequestMethod.POST)
    public Object handleParticipantModifyProcess(
            HttpSession httpSession,
            @PathVariable("participantId") String participantId,
            @ModelAttribute(PARTICIPANT_ATTRIBUTE) ParticipantModel participant,
            BindingResult bindingResult)
    {
        ReservationRequestModel reservationRequest = getReservationRequest(httpSession);
        if (reservationRequest.modifyParticipant(participantId, participant, bindingResult)) {
            return "redirect:" + ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANTS;
        }
        else {
            return handleParticipantModify(Page.PARTICIPANTS, reservationRequest, participant);
        }
    }

    /**
     * Delete existing {@code participant} from given {@code reservationRequest}.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANT_DELETE, method = RequestMethod.GET)
    public Object handleParticipantDelete(
            @PathVariable("participantId") String participantId,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        reservationRequest.deleteParticipant(participantId);
        return "redirect:" + ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANTS;
    }

    /**
     * Show confirmation for creation of a new reservation request.
     *
     * @param reservationRequest to be confirmed
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_CONFIRM, method = RequestMethod.GET)
    public Object handleConfirm(
            UserSession userSession,
            SecurityToken securityToken,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest,
            BindingResult bindingResult)
    {
        ReservationRequestValidator validator = new ReservationRequestValidator(securityToken, reservationService,
                userSession.getLocale(), userSession.getTimeZone());
        validator.validate(reservationRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            return getCreatePermanentRoomCapacityView();
        }
        WizardView wizardView = getWizardView(Page.CONFIRM, "wizardRoomConfirm.jsp");
        wizardView.setNextPageUrl(ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_CONFIRMED);
        wizardView.addAction(ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_CANCEL,
                "views.button.cancel", WizardView.ActionPosition.RIGHT);
        return wizardView;
    }

    /**
     * Create new reservation request and redirect to it's detail.
     *
     * @param reservationRequest to be created
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_CONFIRMED, method = RequestMethod.GET)
    public Object handleConfirmed(
            SecurityToken securityToken,
            SessionStatus sessionStatus,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        // Create or modify reservation request
        String reservationRequestId;
        if (Strings.isNullOrEmpty(reservationRequest.getId())) {
            reservationRequestId = reservationService.createReservationRequest(
                    securityToken, reservationRequest.toApi());
        }
        else {
            reservationRequestId = reservationService.modifyReservationRequest(
                    securityToken, reservationRequest.toApi());
        }
        UserSettingsModel.updateSlotSettings(securityToken, reservationRequest, request, authorizationService);

        // Create user roles
        for (UserRoleModel userRole : reservationRequest.getUserRoles()) {
            if (!Strings.isNullOrEmpty(userRole.getId())) {
                continue;
            }
            userRole.setObjectId(reservationRequestId);
            authorizationService.createAclEntry(securityToken, userRole.toApi());
        }

        // Clear session attributes
        sessionStatus.setComplete();

        // Show detail of newly created reservation request
        return "redirect:" + BackUrl.getInstance(request, ClientWebUrl.WIZARD_ROOM).applyToUrl(
                ClientWebUrl.format(ClientWebUrl.RESERVATION_REQUEST_DETAIL, reservationRequestId)
        );
    }

    /**
     * Cancel the reservation request.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_CANCEL, method = RequestMethod.GET)
    public Object handleCancel(
            SessionStatus sessionStatus)
    {
        sessionStatus.setComplete();
        BackUrl backUrl = BackUrl.getInstance(request, ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY);
        return "redirect:" + backUrl.getUrl(ClientWebUrl.HOME);
    }

    /**
     * Finish the reservation request.
     *
     * @param reservationRequest to be finished
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_FINISH, method = RequestMethod.GET)
    public Object handleFinish(
            SecurityToken securityToken,
            UserSession userSession,
            SessionStatus sessionStatus,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest,
            BindingResult bindingResult)
    {
        ReservationRequestValidator validator = new ReservationRequestValidator(securityToken, reservationService,
                userSession.getLocale(), userSession.getTimeZone());
        validator.validate(reservationRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            return getCreatePermanentRoomCapacityView();
        }
        return handleConfirmed(securityToken, sessionStatus, reservationRequest);
    }

    /**
     * Handle missing session attributes.
     */
    @ExceptionHandler(HttpSessionRequiredException.class)
    public Object handleExceptions(Exception exception)
    {
        logger.warn("Redirecting to " + ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY + ".", exception);
        return "redirect:" + ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY;
    }

    /**
     * @return {@link WizardView} for reservation request form
     */
    private WizardView getCreatePermanentRoomCapacityView()
    {
        WizardView wizardView = getWizardView(Page.ATTRIBUTES, "wizardRoomAttributes.jsp");
        wizardView.setNextPageUrl(WizardController.SUBMIT_RESERVATION_REQUEST);
        wizardView.addAction(WizardController.SUBMIT_RESERVATION_REQUEST_FINISH,
                "views.button.finish", WizardView.ActionPosition.RIGHT);
        wizardView.addAction(ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_CANCEL,
                "views.button.cancel", WizardView.ActionPosition.LEFT);
        return wizardView;
    }
}
