package cz.cesnet.shongo.client.web.controllers;

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
        AbstractWizardCreateController.RESERVATION_REQUEST_ATTRIBUTE,
        AbstractWizardCreateController.PARTICIPANT_ATTRIBUTE,
        "permanentRooms"})
public class WizardCreatePermanentRoomCapacityController extends AbstractWizardCreateController
{
    private static Logger logger = LoggerFactory.getLogger(WizardCreatePermanentRoomCapacityController.class);

    private static final String FORCE_NEW = "new";

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
        wizardView.addPage(new WizardPage(
                Page.CREATE,
                ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY,
                "views.wizard.page.createPermanentRoomCapacity"));
        wizardView.addPage(new WizardPage(
                Page.CREATE_PARTICIPANTS,
                ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_PARTICIPANTS,
                "views.wizard.page.createRoom.participants"));
        wizardView.addPage(new WizardPage(
                Page.CREATE_CONFIRM,
                ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_CONFIRM,
                "views.wizard.page.createConfirm"));
    }

    /**
     * Initialize model editors for additional types.
     *
     * @param binder to be initialized
     */
    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        binder.registerCustomEditor(DateTime.class, new DateTimeEditor());
        binder.registerCustomEditor(LocalDate.class, new LocalDateEditor());
    }

    /**
     * Book capacity for existing permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY, method = RequestMethod.GET)
    public ModelAndView handleAttributes(
            SecurityToken securityToken,
            HttpSession httpSession,
            @RequestParam(value = "permanentRoom", required = false) String permanentRoomId,
            @RequestParam(value = "force", required = false) String force)
    {
        WizardView wizardView = getCreatePermanentRoomCapacityView();

        // Add permanent rooms
        List<ReservationRequestSummary> permanentRooms =
                ReservationRequestModel.getPermanentRooms(reservationService, securityToken, cache);
        wizardView.addObject("permanentRooms", permanentRooms);

        // Add reservation request model
        ReservationRequestModel reservationRequestModel =
                (ReservationRequestModel) httpSession.getAttribute(RESERVATION_REQUEST_ATTRIBUTE);
        if (reservationRequestModel == null || FORCE_NEW.equals(force)) {
            reservationRequestModel = new ReservationRequestModel();
            wizardView.addObject(RESERVATION_REQUEST_ATTRIBUTE, reservationRequestModel);
        }
        reservationRequestModel.setSpecificationType(SpecificationType.PERMANENT_ROOM_CAPACITY);
        reservationRequestModel.setPermanentRoomReservationRequestId(permanentRoomId, permanentRooms);

        return wizardView;
    }

    /**
     * Handle validation of attributes..
     *
     * @param reservationRequest to be validated
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY, method = {RequestMethod.POST})
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
        if (finish) {
            return handleCreateConfirmed(securityToken, sessionStatus, reservationRequest);
        }
        else {
            return "redirect:" + ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_CONFIRM;
        }
    }

    /**
     * Manage participants for permanent room capacity.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_PARTICIPANTS, method = RequestMethod.GET)
    public ModelAndView handleParticipants(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequestModel)
    {
        WizardView wizardView = getWizardView(
                Page.CREATE_PARTICIPANTS, "wizardCreateParticipants.jsp");
        wizardView.addObject("createUrl", ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_PARTICIPANT_CREATE);
        wizardView.addObject("modifyUrl", ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_PARTICIPANT_MODIFY);
        return wizardView;
    }

    /**
     * Show form for adding new participant for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_PARTICIPANT_CREATE, method = RequestMethod.GET)
    public ModelAndView handleParticipantCreate(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        return handleParticipantCreate(Page.CREATE_PARTICIPANTS, reservationRequest);
    }

    /**
     * Store new {@code participant} to given {@code reservationRequest}.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_PARTICIPANT_CREATE, method = RequestMethod.POST)
    public ModelAndView handleParticipantCreateProcess(
            HttpSession httpSession,
            SessionStatus sessionStatus,
            @ModelAttribute(PARTICIPANT_ATTRIBUTE) ParticipantModel participant,
            BindingResult bindingResult)
    {
        ReservationRequestModel reservationRequest = getReservationRequest(httpSession);
        if (createParticipant(reservationRequest, participant, bindingResult)) {
            sessionStatus.setComplete();
            return handleParticipants(reservationRequest);
        }
        else {
            return handleParticipantCreate(Page.CREATE_PARTICIPANTS, reservationRequest, participant);
        }
    }

    /**
     * Show confirmation for creation of a new reservation request.
     *
     * @param reservationRequest to be confirmed
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_CONFIRM, method = RequestMethod.GET)
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
        reservationRequest.loadPermanentRoom(new CacheProvider(cache, securityToken));
        WizardView wizardView = getWizardView(Page.CREATE_CONFIRM, "wizardCreateConfirm.jsp");
        wizardView.setNextPageUrl(ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_CONFIRMED);
        return wizardView;
    }

    /**
     * Create new reservation request and redirect to it's detail.
     *
     * @param reservationRequest to be created
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_CONFIRMED, method = RequestMethod.GET)
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
                    userRole.getUserId(), reservationRequestId, userRole.getRole());
        }

        // Clear session attributes
        sessionStatus.setComplete();

        // Show detail of newly created reservation request
        return "redirect:" + BackUrl.getInstance(request, ClientWebUrl.WIZARD_CREATE_ROOM).applyToUrl(
                ClientWebUrl.format(ClientWebUrl.RESERVATION_REQUEST_DETAIL, reservationRequestId)
        );
    }

    /**
     * Handle missing session attributes.
     */
    @ExceptionHandler(HttpSessionRequiredException.class)
    public Object handleExceptions(Exception exception)
    {
        logger.warn("Redirecting to " + ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY + ".", exception);
        return "redirect:" + ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY;
    }

    /**
     * @return {@link WizardView} for reservation request form
     */
    private WizardView getCreatePermanentRoomCapacityView()
    {
        WizardView wizardView = getWizardView(Page.CREATE, "wizardCreateAttributes.jsp");
        wizardView.setNextPageUrl(WizardCreateController.SUBMIT_RESERVATION_REQUEST);
        wizardView.addAction(WizardCreateController.SUBMIT_RESERVATION_REQUEST_FINISH,
                "views.button.finish", WizardView.ActionPosition.RIGHT);
        return wizardView;
    }
}
