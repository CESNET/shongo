package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.*;
import cz.cesnet.shongo.client.web.editors.DateTimeEditor;
import cz.cesnet.shongo.client.web.editors.LocalDateEditor;
import cz.cesnet.shongo.client.web.models.*;
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

/**
 * Controller for creating a new room
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({"reservationRequest", "userRole"})
public class WizardCreateController extends AbstractWizardController
{
    private static Logger logger = LoggerFactory.getLogger(WizardCreateController.class);

    @Resource
    private Cache cache;

    @Resource
    private ReservationService reservationService;

    @Resource
    private AuthorizationService authorizationService;

    private static enum Page
    {
        CREATE_ROOM,
        CREATE_ROOM_ATTRIBUTES,
        CREATE_ROOM_ROLES,
        CREATE_ROOM_CONFIRM
    }

    @Override
    protected void initWizardPages(WizardView wizardView, Object currentWizardPageId)
    {
        wizardView.addPage(WizardController.createSelectWizardPage());
        wizardView.addPage(new WizardPage(Page.CREATE_ROOM, ClientWebUrl.WIZARD_CREATE_ROOM,
                "views.wizard.page.createRoom"));
        wizardView.addPage(new WizardPage(Page.CREATE_ROOM_ATTRIBUTES, ClientWebUrl.WIZARD_CREATE_ROOM_ATTRIBUTES,
                "views.wizard.page.createRoom.attributes"));
        wizardView.addPage(new WizardPage(Page.CREATE_ROOM_ROLES, ClientWebUrl.WIZARD_CREATE_ROOM_ROLES,
                "views.wizard.page.createRoom.roles"));
        wizardView.addPage(new WizardPage(Page.CREATE_ROOM_CONFIRM, ClientWebUrl.WIZARD_CREATE_ROOM_CONFIRM,
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
    public ModelAndView handleCreateRoom(
            SecurityToken securityToken)
    {
        WizardView wizardView = getWizardView(Page.CREATE_ROOM, "wizardCreateRoom.jsp");
        ReservationRequestModel reservationRequest = new ReservationRequestModel();
        reservationRequest.addUserRole(securityToken.getUserInformation(), Role.OWNER);
        wizardView.addObject("reservationRequest", reservationRequest);
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
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequest)
    {
        reservationRequest.setSpecificationType(SpecificationType.ADHOC_ROOM);
        return "forward:" + ClientWebUrl.WIZARD_CREATE_ROOM_ATTRIBUTES;
    }

    /**
     * Change new videoconference room to permanent type and show form for editing room attributes.
     *
     * @param reservationRequest session attribute is required
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM, method = RequestMethod.GET)
    public String handleCreatePermanentRoom(
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequest)
    {
        reservationRequest.setSpecificationType(SpecificationType.PERMANENT_ROOM);
        return "forward:" + ClientWebUrl.WIZARD_CREATE_ROOM_ATTRIBUTES;
    }

    /**
     * Show form for editing ad-hoc/permanent room attributes.
     *
     * @param reservationRequest session attribute is required
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_ATTRIBUTES, method = RequestMethod.GET)
    public ModelAndView handleCreateRoomAttributes(
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequest)
    {
        if (reservationRequest.getSpecificationType() == null) {
            throw new IllegalStateException("Room type is not specified.");
        }
        return getCreateRoomAttributesView();
    }

    /**
     * @return {@link WizardView} for reservation request form
     */
    private WizardView getCreateRoomAttributesView()
    {
        WizardView wizardView = getWizardView(Page.CREATE_ROOM_ATTRIBUTES, "wizardCreateAttributes.jsp");
        wizardView.addObject("confirmUrl", ClientWebUrl.WIZARD_CREATE_ROOM_ATTRIBUTES_PROCESS);
        wizardView.setNextPageUrl(WizardController.SUBMIT_RESERVATION_REQUEST);
        wizardView.addAction(WizardController.SUBMIT_RESERVATION_REQUEST_FINISH,
                "views.button.finish", WizardView.ActionPosition.RIGHT);
        return wizardView;
    }

    /**
     * Handle validation of attributes..
     *
     * @param reservationRequest to be validated
     */
    @RequestMapping(
            value = ClientWebUrl.WIZARD_CREATE_ROOM_ATTRIBUTES_PROCESS,
            method = {RequestMethod.GET, RequestMethod.POST})
    public Object handleCreateRoomAttributesProcess(
            SecurityToken securityToken,
            SessionStatus sessionStatus,
            @RequestParam(value = "finish", required = false) boolean finish,
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequest,
            BindingResult bindingResult)
    {
        ReservationRequestValidator validator = new ReservationRequestValidator(securityToken, reservationService);
        validator.validate(reservationRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            return getCreateRoomAttributesView();
        }
        if (finish) {
            return handleCreateRoomConfirmed(securityToken, sessionStatus, reservationRequest);
        }
        else {
            return "redirect:" + ClientWebUrl.WIZARD_CREATE_ROOM_ROLES;
        }
    }

    /**
     * Manage user roles for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_ROLES, method = RequestMethod.GET)
    public ModelAndView handleCreateRoomRoles(
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequestModel)
    {
        return getWizardView(Page.CREATE_ROOM_ROLES, "wizardCreateRoomRoles.jsp");
    }

    /**
     * Show form for adding new user role for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_ROLE_CREATE, method = RequestMethod.GET)
    public ModelAndView handleCreateRoomRole(
            SecurityToken securityToken,
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequest)
    {
        ModelAndView modelAndView = getWizardView(Page.CREATE_ROOM_ROLES, "wizardCreateRoomRole.jsp");
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        modelAndView.addObject("userRole", new UserRoleModel(cacheProvider));
        return modelAndView;
    }

    /**
     * Store new user role to ad-hoc/permanent room and forward to {@link #handleCreateRoomRoles}.
     *
     * @param reservationRequest session attribute to which the {@code userRole} will be added
     * @param userRole           to be stored
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_ROLE_CREATE, method = RequestMethod.POST)
    public ModelAndView handleCreateRoomRoleProcess(
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequest,
            @ModelAttribute("userRole") UserRoleModel userRole,
            BindingResult bindingResult)
    {
        UserRoleValidator userRoleValidator = new UserRoleValidator();
        userRoleValidator.validate(userRole, bindingResult);
        if (bindingResult.hasErrors()) {
            // Show form for adding new user role with validation errors
            return getWizardView(Page.CREATE_ROOM_ROLES, "wizardCreateRoomRole.jsp");
        }
        userRole.setTemporaryId();
        reservationRequest.addUserRole(userRole);
        return handleCreateRoomRoles(reservationRequest);
    }

    /**
     * Delete user role with given {@code userRoleId} from given {@code reservationRequest}.
     *
     * @param reservationRequest session attribute to which the {@code userRole} will be added
     * @param userRoleId         of user role to be deleted
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_ROLE_DELETE, method = RequestMethod.GET)
    public ModelAndView handleDeleteRoomRole(
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequest,
            @PathVariable("userRoleId") String userRoleId)
    {
        UserRoleModel userRole = reservationRequest.getUserRole(userRoleId);
        if (userRole == null) {
            throw new IllegalArgumentException("User role " + userRoleId + " doesn't exist.");
        }
        reservationRequest.removeUserRole(userRole);
        return handleCreateRoomRoles(reservationRequest);
    }

    /**
     * Show confirmation for creation of a new reservation request.
     *
     * @param reservationRequest to be confirmed
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_CONFIRM, method = RequestMethod.GET)
    public Object handleCreateRoomConfirm(
            SecurityToken securityToken,
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequest,
            BindingResult bindingResult)
    {
        ReservationRequestValidator validator = new ReservationRequestValidator(securityToken, reservationService);
        validator.validate(reservationRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            return getCreateRoomAttributesView();
        }
        WizardView wizardView = getWizardView(Page.CREATE_ROOM_CONFIRM, "wizardCreateConfirm.jsp");
        wizardView.setNextPageUrl(ClientWebUrl.WIZARD_CREATE_ROOM_CONFIRMED);
        return wizardView;
    }

    /**
     * Create new reservation request and redirect to it's detail.
     *
     * @param reservationRequest to be created
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_CONFIRMED, method = RequestMethod.GET)
    public Object handleCreateRoomConfirmed(
            SecurityToken securityToken,
            SessionStatus sessionStatus,
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequest)
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
        return "redirect:" + ClientWebUrl.format(ClientWebUrl.WIZARD_RESERVATION_REQUEST_DETAIL, reservationRequestId);
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
}
