package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.client.web.*;
import cz.cesnet.shongo.client.web.auth.UserInformationProvider;
import cz.cesnet.shongo.client.web.editors.DateTimeEditor;
import cz.cesnet.shongo.client.web.editors.LocalDateEditor;
import cz.cesnet.shongo.client.web.editors.PeriodEditor;
import cz.cesnet.shongo.client.web.models.ReservationRequestModel;
import cz.cesnet.shongo.client.web.models.ReservationRequestValidator;
import cz.cesnet.shongo.client.web.models.UserRoleModel;
import cz.cesnet.shongo.client.web.models.UserRoleValidator;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Controller for displaying wizard interface.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({"enabledWizardPages", "reservationRequest", "permanentRooms", "userRole"})
public class WizardController
{
    private static Logger logger = LoggerFactory.getLogger(WizardController.class);

    @Resource
    private HttpSession httpSession;

    @Resource
    private ReservationService reservationService;

    @Resource
    private Cache cache;

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
        binder.registerCustomEditor(Period.class, new PeriodEditor());
    }

    /**
     * Forward default wizard page to action selection.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD, method = RequestMethod.GET)
    public String handleDefault()
    {
        return "forward:" + ClientWebUrl.WIZARD_SELECT;
    }

    /**
     * Display list of available actions.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_SELECT, method = RequestMethod.GET)
    public ModelAndView handleSelect(SessionStatus sessionStatus)
    {
        // Clear session attributes
        sessionStatus.setComplete();

        return getWizardView(WizardPage.WIZARD_SELECT);
    }

    /**
     * Display list of reservation requests.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_RESERVATION_REQUEST_LIST, method = RequestMethod.GET)
    public ModelAndView handleReservationRequestList()
    {
        return getWizardView(WizardPage.WIZARD_RESERVATION_REQUEST);
    }

    /**
     * Display detail of reservation request with given {@code reservationRequestId}.
     *
     * @param reservationRequestId
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_RESERVATION_REQUEST_DETAIL, method = RequestMethod.GET)
    public ModelAndView handleReservationRequestDetail(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        ReservationRequestModel reservationRequest = new ReservationRequestModel(
                reservationService.getReservationRequest(securityToken, reservationRequestId));
        return getReservationRequestDetail(reservationRequest, WizardPage.WIZARD_RESERVATION_REQUEST_DETAIL);
    }

    /**
     * Book new videoconference room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM, method = RequestMethod.GET)
    public ModelAndView handleCreateRoom(SecurityToken securityToken, ModelMap modelMap)
    {
        ModelAndView modelAndView = getWizardView(WizardPage.WIZARD_CREATE_ROOM);

        // If reservation request model doesn't exist create it
        if (!modelMap.containsAttribute("reservationRequest")) {
            ReservationRequestModel reservationRequest = new ReservationRequestModel();
            reservationRequest.addUserRole(securityToken.getUserInformation(), Role.OWNER);
            modelAndView.addObject("reservationRequest", reservationRequest);
        }

        return modelAndView;
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
        reservationRequest.setSpecificationType(ReservationRequestModel.SpecificationType.ADHOC_ROOM);
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
        reservationRequest.setSpecificationType(ReservationRequestModel.SpecificationType.PERMANENT_ROOM);
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
        return getWizardView(WizardPage.WIZARD_CREATE_ROOM_ATTRIBUTES);
    }

    /**
     * Manage user roles for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_ROLES, method = RequestMethod.GET)
    public ModelAndView handleCreateRoomRoles(
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequestModel)
    {
        return getWizardView(WizardPage.WIZARD_CREATE_ROOM_ROLES);
    }

    /**
     * Show form for adding new user role for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_ROLE_CREATE, method = RequestMethod.GET)
    public ModelAndView handleCreateRoomRole(SecurityToken securityToken)
    {
        ModelAndView modelAndView = getWizardView(WizardPage.WIZARD_CREATE_ROOM_ROLES);
        UserInformationProvider userInformationProvider = new CacheUserInformationProvider(cache, securityToken);
        modelAndView.addObject("userRole", new UserRoleModel(userInformationProvider));
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
            return getWizardView(WizardPage.WIZARD_CREATE_ROOM_ROLES);
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
        return getWizardView(WizardPage.WIZARD_CREATE_ROOM_ROLES);
    }

    /**
     * Show form for editing permanent room capacity attributes.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY, method = RequestMethod.GET)
    public ModelAndView handleCreatePermanentRoomCapacity(SecurityToken securityToken, ModelMap modelMap)
    {
        ModelAndView modelAndView = getWizardView(WizardPage.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY);

        // If reservation request model doesn't exist create it
        ReservationRequestModel reservationRequestModel = (ReservationRequestModel) modelMap.get("reservationRequest");
        if (reservationRequestModel == null) {
            reservationRequestModel = new ReservationRequestModel();
            modelAndView.addObject("reservationRequest", reservationRequestModel);

        }

        // If permanent rooms doesn't exists create them
        if (!modelMap.containsAttribute("reservationRequest")) {
            modelAndView.addObject("permanentRooms",
                    ReservationRequestModel.getPermanentRooms(reservationService, securityToken, cache));
        }

        // Initialize room
        reservationRequestModel.setSpecificationType(ReservationRequestModel.SpecificationType.PERMANENT_ROOM_CAPACITY);

        return modelAndView;
    }

    /**
     * Handle validation of attributes for ad-hoc/permanent room or permanent room capacity.
     *
     * @param reservationRequest to be validated
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_PROCESS, method = {RequestMethod.GET, RequestMethod.POST})
    public Object handleCreateProcess(
            SecurityToken securityToken,
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequest,
            BindingResult bindingResult)
    {
        WizardPage navigation = validateReservationRequest(securityToken, reservationRequest, bindingResult);
        if (navigation != null) {
            return getWizardView(navigation);
        }

        // Move to next wizard page, because reservation request is valid
        switch (reservationRequest.getSpecificationType()) {
            case ADHOC_ROOM:
            case PERMANENT_ROOM:
                return "redirect:" + ClientWebUrl.WIZARD_CREATE_ROOM_ROLES;
            case PERMANENT_ROOM_CAPACITY:
                return "redirect:" + ClientWebUrl.WIZARD_CREATE_CONFIRM;
            default:
                throw new TodoImplementException(reservationRequest.getSpecificationType().toString());
        }
    }

    /**
     * Show confirmation for creation of a new reservation request.
     *
     * @param reservationRequest to be confirmed
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_CONFIRM, method = RequestMethod.GET)
    public Object handleCreateConfirm(
            SecurityToken securityToken,
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequest,
            BindingResult bindingResult)
    {
        WizardPage navigation = validateReservationRequest(securityToken, reservationRequest, bindingResult);
        if (navigation != null) {
            // Show page for correcting validation errors
            return getWizardView(navigation);
        }
        // Show confirmation page
        switch (reservationRequest.getSpecificationType()) {
            case ADHOC_ROOM:
            case PERMANENT_ROOM:
                return getWizardView(WizardPage.WIZARD_CREATE_ROOM_CONFIRM);
            case PERMANENT_ROOM_CAPACITY:
                return getWizardView(WizardPage.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_CONFIRM);
            default:
                throw new TodoImplementException(reservationRequest.getSpecificationType().toString());
        }
    }

    /**
     * Create new reservation request and redirect to it's detail.
     *
     * @param reservationRequest to be created
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_CONFIRMED, method = RequestMethod.GET)
    public Object handleCreateConfirmed(
            SecurityToken securityToken,
            SessionStatus sessionStatus,
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequest)
    {
        // TODO: Create reservation request
        reservationRequest.setId("TODO: create");

        // Clear session attributes
        sessionStatus.setComplete();

        // Show detail of newly created reservation request
        String reservationRequestId = reservationRequest.getId();
        return "redirect:" + ClientWebUrl.WIZARD_CREATE_DETAIL + "?reservationRequestId=" + reservationRequestId;
    }

    /**
     * Show detail of newly created reservation request.
     *
     * @param reservationRequestId to be displayed
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_DETAIL, method = RequestMethod.GET)
    public Object handleCreateDetail(
            SecurityToken securityToken,
            @RequestParam(value = "reservationRequestId") String reservationRequestId)
    {
        ReservationRequestModel reservationRequest = new ReservationRequestModel(
                reservationService.getReservationRequest(securityToken, reservationRequestId));
        switch (reservationRequest.getSpecificationType()) {
            case ADHOC_ROOM:
            case PERMANENT_ROOM:
                return getReservationRequestDetail(reservationRequest, WizardPage.WIZARD_CREATE_ROOM_DETAIL);
            case PERMANENT_ROOM_CAPACITY:
                return getReservationRequestDetail(reservationRequest,
                        WizardPage.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_DETAIL);
            default:
                throw new TodoImplementException(reservationRequest.getSpecificationType().toString());
        }
    }

    /**
     * Handle missing session attributes.
     */
    @ExceptionHandler(HttpSessionRequiredException.class)
    public Object handleExceptions(Exception exception) {

        logger.warn("Redirecting to action selection because session attribute is missing.", exception);
        return "redirect:" + ClientWebUrl.WIZARD_SELECT;
    }

    /**
     * Validate given {@code reservationRequest}.
     *
     * @param securityToken
     * @param reservationRequest
     * @param bindingResult
     * @return null if validation succeeds,
     *         {@link ClientWebNavigation} for displaying validation errors otherwise
     */
    private WizardPage validateReservationRequest(SecurityToken securityToken,
            ReservationRequestModel reservationRequest, BindingResult bindingResult)
    {
        ReservationRequestValidator validator = new ReservationRequestValidator(securityToken, reservationService);
        validator.validate(reservationRequest, bindingResult);
        if (!bindingResult.hasErrors()) {
            return null;
        }

        // Return navigation for displaying form for editing attributes with validation errors
        switch (reservationRequest.getSpecificationType()) {
            case ADHOC_ROOM:
            case PERMANENT_ROOM:
                return WizardPage.WIZARD_CREATE_ROOM_ATTRIBUTES;
            case PERMANENT_ROOM_CAPACITY:
                return WizardPage.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY;
            default:
                throw new TodoImplementException(reservationRequest.getSpecificationType().toString());
        }
    }

    /**
     * Get wizard view for detail of given {@code reservationRequest}.
     *
     * @param reservationRequest
     * @param wizardPage
     * @return initialized {@link ModelAndView}
     */
    public ModelAndView getReservationRequestDetail(ReservationRequestModel reservationRequest, WizardPage wizardPage)
    {
        ModelAndView modelAndView = getWizardView(wizardPage);
        modelAndView.addObject("reservationRequest", reservationRequest);
        return modelAndView;
    }

    /**
     * Get wizard view for given {@code wizardPage}.
     *
     * @param currentPage {@link WizardPage} for which the wizard should be displayed
     * @return initialized {@link ModelAndView}
     */
    private ModelAndView getWizardView(WizardPage currentPage)
    {
        if (currentPage == null) {
            throw new IllegalArgumentException("Current page must not be null.");
        }
        @SuppressWarnings("unchecked")
        Set<WizardPage> enabledWizardPages = (Set) httpSession.getAttribute("enabledWizardPages");
        if (enabledWizardPages == null) {
            enabledWizardPages = new HashSet<WizardPage>();
        }

        List<WizardPage> wizardPages = new LinkedList<WizardPage>();
        // Add current and previous pages
        WizardPage previousPage = currentPage;
        boolean enabled = true;
        while (previousPage != null) {
            wizardPages.add(0, previousPage);
            if (enabled || previousPage.isAlwaysEnabled()) {
                enabledWizardPages.add(previousPage);
            }
            if (!previousPage.isPreviousEnabled()) {
                enabled = false;
            }
            previousPage = previousPage.getPreviousPage();
        }
        // Add next pages
        WizardPage nextPage = currentPage.getNextPage();
        while (nextPage != null) {
            wizardPages.add(nextPage);
            nextPage = nextPage.getNextPage();
        }
        ModelAndView modelAndView = new ModelAndView("wizard");
        modelAndView.addObject("wizardPages", wizardPages);
        modelAndView.addObject("enabledWizardPages", enabledWizardPages);
        modelAndView.addObject("currentPage", currentPage);
        return modelAndView;
    }
}
