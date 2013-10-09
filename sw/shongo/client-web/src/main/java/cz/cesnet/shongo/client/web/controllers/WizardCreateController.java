package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.WizardPage;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.BackUrl;
import cz.cesnet.shongo.client.web.support.editors.DateTimeEditor;
import cz.cesnet.shongo.client.web.support.editors.LocalDateEditor;
import cz.cesnet.shongo.controller.Role;
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

/**
 * Controller for creating a new room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({
        AbstractWizardCreateController.RESERVATION_REQUEST_ATTRIBUTE,
        AbstractWizardCreateController.PARTICIPANT_ATTRIBUTE,
        "userRole"
})
public class WizardCreateController extends AbstractWizardCreateController
{
    private static Logger logger = LoggerFactory.getLogger(WizardCreateController.class);

    public static final String SUBMIT_RESERVATION_REQUEST = "javascript: " +
            "document.getElementById('reservationRequest').submit();";

    public static final String SUBMIT_RESERVATION_REQUEST_FINISH = "javascript: " +
            "$('form#reservationRequest').append('<input type=\\'hidden\\' name=\\'finish\\' value=\\'true\\'/>');" +
            "document.getElementById('reservationRequest').submit();";

    @Resource
    private Cache cache;

    @Resource
    private ReservationService reservationService;

    @Resource
    private AuthorizationService authorizationService;

    private static enum Page
    {
        CREATE,
        CREATE_ATTRIBUTES,
        CREATE_ROLES,
        CREATE_PARTICIPANTS,
        CREATE_CONFIRM
    }

    @Override
    protected void initWizardPages(WizardView wizardView, Object currentWizardPageId)
    {
        wizardView.addPage(new WizardPage(Page.CREATE, ClientWebUrl.WIZARD_CREATE_ROOM,
                "views.wizard.page.createRoom"));
        wizardView.addPage(new WizardPage(Page.CREATE_ATTRIBUTES, ClientWebUrl.WIZARD_CREATE_ROOM_ATTRIBUTES,
                "views.wizard.page.createRoom.attributes"));
        wizardView.addPage(new WizardPage(Page.CREATE_ROLES, ClientWebUrl.WIZARD_CREATE_ROOM_ROLES,
                "views.wizard.page.createRoom.roles"));
        wizardView.addPage(new WizardPage(Page.CREATE_PARTICIPANTS, ClientWebUrl.WIZARD_CREATE_ROOM_PARTICIPANTS,
                "views.wizard.page.createRoom.participants"));
        wizardView.addPage(new WizardPage(Page.CREATE_CONFIRM, ClientWebUrl.WIZARD_CREATE_ROOM_CONFIRM,
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
     * Book new videoconference room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM, method = RequestMethod.GET)
    public ModelAndView handleCreate(
            SecurityToken securityToken)
    {
        WizardView wizardView = getWizardView(Page.CREATE, "wizardCreateRoom.jsp");
        ReservationRequestModel reservationRequest = new ReservationRequestModel();
        reservationRequest.addUserRole(securityToken.getUserInformation(), Role.OWNER);
        wizardView.addObject(RESERVATION_REQUEST_ATTRIBUTE, reservationRequest);
        wizardView.setNextPageUrl(null);
        return wizardView;
    }

    /**
     * Change new videoconference room to ad-hoc type and show form for editing room attributes.
     *
     * @param reservationRequest session attribute is required
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ADHOC_ROOM, method = RequestMethod.GET)
    public String handleCreateAdhocRoom(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        reservationRequest.setSpecificationType(SpecificationType.ADHOC_ROOM);
        return "redirect:" + ClientWebUrl.WIZARD_CREATE_ROOM_ATTRIBUTES;
    }

    /**
     * Change new videoconference room to permanent type and show form for editing room attributes.
     *
     * @param reservationRequest session attribute is required
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM, method = RequestMethod.GET)
    public String handleCreatePermanentRoom(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        reservationRequest.setSpecificationType(SpecificationType.PERMANENT_ROOM);
        return "redirect:" + ClientWebUrl.WIZARD_CREATE_ROOM_ATTRIBUTES;
    }

    /**
     * Show form for editing ad-hoc/permanent room attributes.
     *
     * @param reservationRequest session attribute is required
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_ATTRIBUTES, method = RequestMethod.GET)
    public ModelAndView handleAttributes(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        if (reservationRequest.getSpecificationType() == null) {
            throw new IllegalStateException("Room type is not specified.");
        }
        return getCreateRoomAttributesView();
    }

    /**
     * Handle validation of attributes..
     *
     * @param reservationRequest to be validated
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_ATTRIBUTES, method = {RequestMethod.POST})
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
            return getCreateRoomAttributesView();
        }
        if (finish) {
            return handleConfirmed(securityToken, sessionStatus, reservationRequest);
        }
        else {
            return "redirect:" + ClientWebUrl.WIZARD_CREATE_ROOM_ROLES;
        }
    }

    /**
     * Manage user roles for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_ROLES, method = RequestMethod.GET)
    public ModelAndView handleRoles(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequestModel)
    {
        return getWizardView(Page.CREATE_ROLES, "wizardCreateRoomRoles.jsp");
    }

    /**
     * Show form for adding new user role for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_ROLE_CREATE, method = RequestMethod.GET)
    public ModelAndView handleRoleCreate(
            SecurityToken securityToken,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        WizardView wizardView = getWizardView(Page.CREATE_ROLES, "wizardCreateRoomRole.jsp");
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        wizardView.addObject("userRole", new UserRoleModel(cacheProvider));
        wizardView.setNextPageUrl(null);
        wizardView.setPreviousPageUrl(null);
        return wizardView;
    }

    /**
     * Store new user role to ad-hoc/permanent room and forward to {@link #handleRoles}.
     *
     * @param httpSession
     * @param userRole    to be stored
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_ROLE_CREATE, method = RequestMethod.POST)
    public ModelAndView handleRoleCreateProcess(
            HttpSession httpSession,
            @ModelAttribute("userRole") UserRoleModel userRole,
            BindingResult bindingResult)
    {
        UserRoleValidator userRoleValidator = new UserRoleValidator();
        userRoleValidator.validate(userRole, bindingResult);
        if (bindingResult.hasErrors()) {
            // Show form for adding new user role with validation errors
            WizardView wizardView = getWizardView(Page.CREATE_ROLES, "wizardCreateRoomRole.jsp");
            wizardView.setNextPageUrl(null);
            wizardView.setPreviousPageUrl(null);
            return wizardView;
        }
        userRole.setTemporaryId();
        userRole.setDeletable(true);
        ReservationRequestModel reservationRequest = getReservationRequest(httpSession);
        reservationRequest.addUserRole(userRole);
        return handleRoles(reservationRequest);
    }

    /**
     * Delete user role with given {@code userRoleId} from given {@code reservationRequest}.
     *
     * @param reservationRequest session attribute to which the {@code userRole} will be added
     * @param userRoleId         of user role to be deleted
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_ROLE_DELETE, method = RequestMethod.GET)
    public ModelAndView handleRoleDelete(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest,
            @PathVariable("aclRecordId") String userRoleId)
    {
        UserRoleModel userRole = reservationRequest.getUserRole(userRoleId);
        if (userRole == null) {
            throw new IllegalArgumentException("User role " + userRoleId + " doesn't exist.");
        }
        reservationRequest.removeUserRole(userRole);
        return handleRoles(reservationRequest);
    }

    /**
     * Manage participants for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_PARTICIPANTS, method = RequestMethod.GET)
    public ModelAndView handleParticipants(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequestModel)
    {
        WizardView wizardView = getWizardView(Page.CREATE_PARTICIPANTS, "wizardCreateParticipants.jsp");
        wizardView.addObject("createUrl", ClientWebUrl.WIZARD_CREATE_ROOM_PARTICIPANTS_CREATE);
        wizardView.addObject("modifyUrl", ClientWebUrl.WIZARD_CREATE_ROOM_PARTICIPANTS_MODIFY);
        return wizardView;
    }

    /**
     * Show form for adding new participant for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_PARTICIPANTS_CREATE, method = RequestMethod.GET)
    public ModelAndView handleParticipantCreate(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        return handleParticipantCreate(Page.CREATE_PARTICIPANTS, reservationRequest);
    }

    /**
     * Store new {@code participant} to given {@code reservationRequest}.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_PARTICIPANTS_CREATE, method = RequestMethod.POST)
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
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_CONFIRM, method = RequestMethod.GET)
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
            return getCreateRoomAttributesView();
        }
        WizardView wizardView = getWizardView(Page.CREATE_CONFIRM, "wizardCreateConfirm.jsp");
        wizardView.setNextPageUrl(ClientWebUrl.WIZARD_CREATE_ROOM_CONFIRMED);
        return wizardView;
    }

    /**
     * Create new reservation request and redirect to it's detail.
     *
     * @param reservationRequest to be created
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_CONFIRMED, method = RequestMethod.GET)
    public Object handleConfirmed(
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
    @ExceptionHandler({HttpSessionRequiredException.class, IllegalStateException.class})
    public Object handleExceptions(Exception exception)
    {
        logger.warn("Redirecting to " + ClientWebUrl.WIZARD_CREATE_ROOM + ".", exception);
        return "redirect:" + ClientWebUrl.WIZARD_CREATE_ROOM;
    }

    /**
     * @return {@link WizardView} for reservation request form
     */
    private WizardView getCreateRoomAttributesView()
    {
        WizardView wizardView = getWizardView(Page.CREATE_ATTRIBUTES, "wizardCreateAttributes.jsp");
        wizardView.setNextPageUrl(SUBMIT_RESERVATION_REQUEST);
        wizardView.addAction(SUBMIT_RESERVATION_REQUEST_FINISH,
                "views.button.finish", WizardView.ActionPosition.RIGHT);
        return wizardView;
    }
}
