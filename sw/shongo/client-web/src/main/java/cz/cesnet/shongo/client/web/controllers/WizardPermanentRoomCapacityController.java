package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.WizardPage;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.BackUrl;
import cz.cesnet.shongo.client.web.support.editors.DateTimeEditor;
import cz.cesnet.shongo.client.web.support.editors.LocalDateEditor;
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
        CREATE,
        CREATE_PARTICIPANTS,
        CREATE_CONFIRM,
    }

    @Override
    protected void initWizardPages(WizardView wizardView, Object currentWizardPageId)
    {
        ReservationRequestModel reservationRequest =
                (ReservationRequestModel) WebUtils.getSessionAttribute(request, RESERVATION_REQUEST_ATTRIBUTE);

        wizardView.addPage(new WizardPage(
                Page.CREATE,
                ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY,
                "views.wizard.page.createPermanentRoomCapacity"));
        if (reservationRequest == null || reservationRequest.getTechnology() == null
                || reservationRequest.getTechnology().equals(TechnologyModel.ADOBE_CONNECT)) {
            wizardView.addPage(new WizardPage(
                    Page.CREATE_PARTICIPANTS,
                    ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANTS,
                    "views.wizard.page.createRoom.participants"));
        }
        wizardView.addPage(new WizardPage(
                Page.CREATE_CONFIRM,
                ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_CONFIRM,
                "views.wizard.page.createConfirm"));
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
     * Book capacity for existing permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY, method = RequestMethod.GET)
    public ModelAndView handleAttributes(
            SecurityToken securityToken,
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
            reservationRequest = new ReservationRequestModel(new CacheProvider(cache, securityToken));
            wizardView.addObject(RESERVATION_REQUEST_ATTRIBUTE, reservationRequest);
        }
        reservationRequest.setTechnology(null);
        reservationRequest.setSpecificationType(SpecificationType.PERMANENT_ROOM_CAPACITY);
        if (permanentRoomId != null) {
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
        if (!reservationRequest.hasUserParticipant(securityToken.getUserId(), ParticipantRole.ADMINISTRATOR)) {
            reservationRequest.addRoomParticipant(securityToken.getUserInformation(), ParticipantRole.ADMINISTRATOR);
        }
        if (finish) {
            return handleCreateConfirmed(securityToken, sessionStatus, reservationRequest);
        }
        else {
            if (reservationRequest.getTechnology().equals(TechnologyModel.ADOBE_CONNECT)) {
                return "redirect:" + ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANTS;
            }
            else {
                return "redirect:" + ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_CONFIRM;
            }
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
                Page.CREATE_PARTICIPANTS, "wizardCreateParticipants.jsp");
        wizardView.addObject("createUrl", ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANT_CREATE);
        wizardView.addObject("modifyUrl", ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANT_MODIFY);
        wizardView.addObject("deleteUrl", ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANT_DELETE);
        return wizardView;
    }

    /**
     * Show form for adding new participant for permanent room capacity.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANT_CREATE, method = RequestMethod.GET)
    public ModelAndView handleParticipantCreate(
            SecurityToken securityToken,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        return handleParticipantCreate(Page.CREATE_PARTICIPANTS, reservationRequest, securityToken);
    }

    /**
     * Store new {@code participant} to given {@code reservationRequest}.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANT_CREATE, method = RequestMethod.POST)
    public ModelAndView handleParticipantCreateProcess(
            HttpSession httpSession,
            @ModelAttribute(PARTICIPANT_ATTRIBUTE) ParticipantModel participant,
            BindingResult bindingResult)
    {
        ReservationRequestModel reservationRequest = getReservationRequest(httpSession);
        if (reservationRequest.createParticipant(participant, bindingResult)) {
            return handleParticipants(reservationRequest);
        }
        else {
            return handleParticipantView(Page.CREATE_PARTICIPANTS, reservationRequest, participant);
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
        return handleParticipantModify(Page.CREATE_PARTICIPANTS, reservationRequest, participantId);
    }

    /**
     * Store changes for existing {@code participant} to given {@code reservationRequest}.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANT_MODIFY, method = RequestMethod.POST)
    public ModelAndView handleParticipantModifyProcess(
            HttpSession httpSession,
            @PathVariable("participantId") String participantId,
            @ModelAttribute(PARTICIPANT_ATTRIBUTE) ParticipantModel participant,
            BindingResult bindingResult)
    {
        ReservationRequestModel reservationRequest = getReservationRequest(httpSession);
        if (reservationRequest.modifyParticipant(participantId, participant, bindingResult)) {
            return handleParticipants(reservationRequest);
        }
        else {
            return handleParticipantModify(Page.CREATE_PARTICIPANTS, reservationRequest, participant);
        }
    }

    /**
     * Delete existing {@code participant} from given {@code reservationRequest}.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_PARTICIPANT_DELETE, method = RequestMethod.GET)
    public ModelAndView handleParticipantDelete(
            @PathVariable("participantId") String participantId,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        reservationRequest.deleteParticipant(participantId);
        return handleParticipants(reservationRequest);
    }

    /**
     * Show confirmation for creation of a new reservation request.
     *
     * @param reservationRequest to be confirmed
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_CONFIRM, method = RequestMethod.GET)
    public Object handleCreateConfirm(
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
        WizardView wizardView = getWizardView(Page.CREATE_CONFIRM, "wizardCreateConfirm.jsp");
        wizardView.setNextPageUrl(ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_CONFIRMED);
        return wizardView;
    }

    /**
     * Create new reservation request and redirect to it's detail.
     *
     * @param reservationRequest to be created
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY_CONFIRMED, method = RequestMethod.GET)
    public Object handleCreateConfirmed(
            SecurityToken securityToken,
            SessionStatus sessionStatus,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        // Create reservation request
        String reservationRequestId = reservationService.createReservationRequest(
                securityToken, reservationRequest.toApi());

        // Create user roles
        for (UserRoleModel userRole : reservationRequest.getUserRoles()) {
            authorizationService.createAclRecord(securityToken,
                    userRole.getUserId(), reservationRequestId, userRole.getEntityRole());
        }

        // Clear session attributes
        sessionStatus.setComplete();

        // Show detail of newly created reservation request
        return "redirect:" + BackUrl.getInstance(request, ClientWebUrl.WIZARD_ROOM).applyToUrl(
                ClientWebUrl.format(ClientWebUrl.RESERVATION_REQUEST_DETAIL, reservationRequestId)
        );
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
        WizardView wizardView = getWizardView(Page.CREATE, "wizardCreateAttributes.jsp");
        wizardView.setNextPageUrl(WizardRoomController.SUBMIT_RESERVATION_REQUEST);
        wizardView.addAction(WizardRoomController.SUBMIT_RESERVATION_REQUEST_FINISH,
                "views.button.finish", WizardView.ActionPosition.RIGHT);
        return wizardView;
    }
}
