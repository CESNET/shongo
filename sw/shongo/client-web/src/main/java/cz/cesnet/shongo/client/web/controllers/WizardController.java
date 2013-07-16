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
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
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
@SessionAttributes({"availablePages", "reservationRequest", "permanentRooms", "userRole"})
public class WizardController
{
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

        return handleWizardView(ClientWebNavigation.WIZARD_SELECT);
    }

    /**
     * Display list of reservation requests.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_RESERVATION_REQUEST_LIST, method = RequestMethod.GET)
    public ModelAndView handleReservationRequestList()
    {
        return handleWizardView(ClientWebNavigation.WIZARD_RESERVATION_REQUEST);
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
        return handleWizardView(ClientWebNavigation.WIZARD_RESERVATION_REQUEST_DETAIL);
    }

    /**
     * Book new videoconference room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM, method = RequestMethod.GET)
    public ModelAndView handleCreateRoom(SecurityToken securityToken, ModelMap modelMap)
    {
        ModelAndView modelAndView = handleWizardView(ClientWebNavigation.WIZARD_CREATE_ROOM);

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
        return handleWizardView(ClientWebNavigation.WIZARD_CREATE_ROOM_ATTRIBUTES);
    }

    /**
     * Manage user roles for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_ROLES, method = RequestMethod.GET)
    public ModelAndView handleCreateRoomRoles(
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequestModel)
    {
        return handleWizardView(ClientWebNavigation.WIZARD_CREATE_ROOM_ROLES);
    }

    /**
     * Show form for adding new user role for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_ROOM_ROLE_CREATE, method = RequestMethod.GET)
    public ModelAndView handleCreateRoomRole(SecurityToken securityToken)
    {
        ModelAndView modelAndView = handleWizardView(ClientWebNavigation.WIZARD_CREATE_ROOM_ROLES);
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
            return handleWizardView(ClientWebNavigation.WIZARD_CREATE_ROOM_ROLES);
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
        return handleWizardView(ClientWebNavigation.WIZARD_CREATE_ROOM_ROLES);
    }

    /**
     * Show form for editing permanent room capacity attributes.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY, method = RequestMethod.GET)
    public ModelAndView handleCreatePermanentRoomCapacity(SecurityToken securityToken)
    {
        ModelAndView modelAndView = handleWizardView(ClientWebNavigation.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY);
        ReservationRequestModel reservationRequestModel = new ReservationRequestModel();
        reservationRequestModel.setSpecificationType(ReservationRequestModel.SpecificationType.PERMANENT_ROOM_CAPACITY);
        modelAndView.addObject("reservationRequest", reservationRequestModel);
        modelAndView.addObject("permanentRooms",
                ReservationRequestModel.getPermanentRooms(reservationService, securityToken, cache));
        return modelAndView;
    }

    /**
     * Handle validation of attributes for ad-hoc/permanent room or permanent room capacity.
     *
     * @param reservationRequest to be validated
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_CONFIRM, method = {RequestMethod.GET, RequestMethod.POST})
    public Object handleCreateConfirm(
            SecurityToken securityToken,
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequest,
            BindingResult bindingResult)
    {
        ClientWebNavigation navigation = validateReservationRequest(securityToken, reservationRequest, bindingResult);
        if (navigation != null) {
            return handleWizardView(navigation);
        }

        // Move to next wizard page, because reservation request is valid
        switch (reservationRequest.getSpecificationType()) {
            case ADHOC_ROOM:
            case PERMANENT_ROOM:
                return "redirect:" + ClientWebUrl.WIZARD_CREATE_ROOM_ROLES;
            case PERMANENT_ROOM_CAPACITY:
                return "redirect:" + ClientWebUrl.WIZARD_CREATE_FINISH;
            default:
                throw new TodoImplementException(reservationRequest.getSpecificationType().toString());
        }
    }

    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_FINISH, method = RequestMethod.GET)
    public ModelAndView handleCreateRoomFinish(
            SecurityToken securityToken,
            SessionStatus sessionStatus,
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequest,
            BindingResult bindingResult)
    {
        ClientWebNavigation navigation = validateReservationRequest(securityToken, reservationRequest, bindingResult);
        if (navigation != null) {
            return handleWizardView(navigation);
        }

        // Clear session attributes
        sessionStatus.setComplete();

        // TODO: Create reservation request
        reservationRequest.setId("TODO: create");

        ModelAndView modelAndView;
        switch (reservationRequest.getSpecificationType()) {
            case ADHOC_ROOM:
            case PERMANENT_ROOM:
                modelAndView = handleWizardView(ClientWebNavigation.WIZARD_CREATE_ROOM_FINISH);
                break;
            case PERMANENT_ROOM_CAPACITY:
                modelAndView = handleWizardView(ClientWebNavigation.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_FINISH);
                break;
            default:
                throw new TodoImplementException(reservationRequest.getSpecificationType().toString());
        }
        modelAndView.addObject("reservationRequest", reservationRequest);
        return modelAndView;
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
    private ClientWebNavigation validateReservationRequest(SecurityToken securityToken,
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
                return ClientWebNavigation.WIZARD_CREATE_ROOM_ATTRIBUTES;
            case PERMANENT_ROOM_CAPACITY:
                return ClientWebNavigation.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY;
            default:
                throw new TodoImplementException(reservationRequest.getSpecificationType().toString());
        }
    }

    /**
     * Wizard page which represents multiple pages from which the user will select one.
     */
    private static final Page PAGE_OTHER = new Page(null, "views.wizard.page.other");

    /**
     * Display wizard view for given {@code navigation}.
     *
     * @param navigation current {@link ClientWebNavigation} for which the wizard should be displayed
     * @return initialized {@link ModelAndView}
     */
    private ModelAndView handleWizardView(ClientWebNavigation navigation)
    {
        @SuppressWarnings("unchecked")
        Set<Page> availablePages = (Set) httpSession.getAttribute("availablePages");
        if (availablePages == null) {
            availablePages = new HashSet<Page>();
        }

        Page currentPage = navigation.getPage();

        // Get
        List<Page> wizardPages = new LinkedList<Page>();
        wizardPages.add(currentPage);
        availablePages.add(currentPage);
        Page page = currentPage.getParentPage();
        while (page != null) {
            wizardPages.add(0, page);
            availablePages.add(page);
            page = page.getParentPage();
        }
        page = currentPage;
        while (page != null) {
            List<Page> childPages = page.getChildPages();
            int childPageCount = childPages.size();
            if (childPageCount == 0) {
                break;
            }
            else if (childPageCount == 1) {
                page = childPages.get(0);
                wizardPages.add(page);
            }
            else {
                wizardPages.add(PAGE_OTHER);
                break;
            }
        }
        ModelAndView modelAndView = new ModelAndView("wizard");
        modelAndView.addObject("navigation", navigation);
        modelAndView.addObject("wizardPages", wizardPages);
        modelAndView.addObject("availablePages", availablePages);
        return modelAndView;
    }
}
