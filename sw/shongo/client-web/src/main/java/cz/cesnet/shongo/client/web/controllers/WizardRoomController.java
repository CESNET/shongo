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
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.api.AclEntry;
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

/**
 * Controller for creating a new room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({
        WizardParticipantsController.RESERVATION_REQUEST_ATTRIBUTE,
        WizardParticipantsController.PARTICIPANT_ATTRIBUTE,
        "userRole"
})
public class WizardRoomController extends WizardParticipantsController
{
    private static Logger logger = LoggerFactory.getLogger(WizardRoomController.class);

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
        ROOM_TYPE,
        ROOM_ATTRIBUTES,
        ROOM_ROLES,
        ROOM_PARTICIPANTS,
        ROOM_CONFIRM
    }

    @Override
    protected void initWizardPages(WizardView wizardView, Object currentWizardPageId)
    {
        ReservationRequestModel reservationRequest =
                (ReservationRequestModel) WebUtils.getSessionAttribute(request, RESERVATION_REQUEST_ATTRIBUTE);

        wizardView.addPage(new WizardPage(Page.ROOM_TYPE, ClientWebUrl.WIZARD_ROOM,
                "views.wizard.page.createRoom"));
        wizardView.addPage(new WizardPage(Page.ROOM_ATTRIBUTES, ClientWebUrl.WIZARD_ROOM_ATTRIBUTES,
                "views.wizard.page.createRoom.attributes"));
        if (reservationRequest == null || reservationRequest.getSpecificationType() == null
                || reservationRequest.getSpecificationType().equals(SpecificationType.PERMANENT_ROOM)) {
            wizardView.addPage(new WizardPage(Page.ROOM_ROLES, ClientWebUrl.WIZARD_ROOM_ROLES,
                    "views.wizard.page.createRoom.roles"));
        }
        if (reservationRequest == null || reservationRequest.getTechnology() == null
                || reservationRequest.getTechnology().equals(TechnologyModel.ADOBE_CONNECT)) {
            wizardView.addPage(new WizardPage(Page.ROOM_PARTICIPANTS, ClientWebUrl.WIZARD_ROOM_PARTICIPANTS,
                    "views.wizard.page.createRoom.participants"));
        }
        wizardView.addPage(new WizardPage(Page.ROOM_CONFIRM, ClientWebUrl.WIZARD_ROOM_CONFIRM,
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
     * Book new virtual room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM, method = RequestMethod.GET)
    public ModelAndView handleRoomType(SecurityToken securityToken)
    {
        WizardView wizardView = getWizardView(Page.ROOM_TYPE, "wizardCreateRoom.jsp");
        ReservationRequestModel reservationRequest =
                new ReservationRequestModel(new CacheProvider(cache, securityToken));
        reservationRequest.addUserRole(securityToken.getUserInformation(), ObjectRole.OWNER);
        reservationRequest.addRoomParticipant(securityToken.getUserInformation(), ParticipantRole.ADMINISTRATOR);
        wizardView.addObject(RESERVATION_REQUEST_ATTRIBUTE, reservationRequest);
        wizardView.setNextPageUrl(null);
        return wizardView;
    }

    /**
     * Change new virtual room to ad-hoc type and show form for editing room attributes.
     *
     * @param reservationRequest session attribute is required
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ADHOC_ROOM, method = RequestMethod.GET)
    public String handleAdhocRoom(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        reservationRequest.setSpecificationType(SpecificationType.ADHOC_ROOM);
        return "redirect:" + ClientWebUrl.WIZARD_ROOM_ATTRIBUTES;
    }

    /**
     * Change new virtual room to permanent type and show form for editing room attributes.
     *
     * @param reservationRequest session attribute is required
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PERMANENT_ROOM, method = RequestMethod.GET)
    public String handlePermanentRoom(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        reservationRequest.setSpecificationType(SpecificationType.PERMANENT_ROOM);
        return "redirect:" + ClientWebUrl.WIZARD_ROOM_ATTRIBUTES;
    }

    /**
     * Show form for editing ad-hoc/permanent room attributes.
     *
     * @param reservationRequest session attribute is required
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_ATTRIBUTES, method = RequestMethod.GET)
    public ModelAndView handleRoomAttributes(
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
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_ATTRIBUTES, method = {RequestMethod.POST})
    public Object handleRoomAttributesProcess(
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
            if (reservationRequest.getSpecificationType().equals(SpecificationType.PERMANENT_ROOM)) {
                return "redirect:" + ClientWebUrl.WIZARD_ROOM_ROLES;
            }
            else if (reservationRequest.getTechnology().equals(TechnologyModel.ADOBE_CONNECT)) {
                return "redirect:" + ClientWebUrl.WIZARD_ROOM_PARTICIPANTS;
            }
            else {
                return "redirect:" + ClientWebUrl.WIZARD_ROOM_CONFIRM;
            }
        }
    }

    /**
     * Manage user roles for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_ROLES, method = RequestMethod.GET)
    public ModelAndView handleRoles(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        return getWizardView(Page.ROOM_ROLES, "wizardCreateRoomRoles.jsp");
    }

    /**
     * Show form for adding new user role for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_ROLE_CREATE, method = RequestMethod.GET)
    public ModelAndView handleRoleCreate(
            SecurityToken securityToken,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        WizardView wizardView = getWizardView(Page.ROOM_ROLES, "wizardCreateRoomRole.jsp");
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
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_ROLE_CREATE, method = RequestMethod.POST)
    public ModelAndView handleRoleCreateProcess(
            HttpSession httpSession,
            @ModelAttribute("userRole") UserRoleModel userRole,
            BindingResult bindingResult)
    {
        UserRoleValidator userRoleValidator = new UserRoleValidator();
        userRoleValidator.validate(userRole, bindingResult);
        if (bindingResult.hasErrors()) {
            // Show form for adding new user role with validation errors
            WizardView wizardView = getWizardView(Page.ROOM_ROLES, "wizardCreateRoomRole.jsp");
            wizardView.setNextPageUrl(null);
            wizardView.setPreviousPageUrl(null);
            return wizardView;
        }
        userRole.setNewId();
        userRole.setDeletable(true);
        ReservationRequestModel reservationRequest = getReservationRequest(httpSession);
        reservationRequest.addUserRole(userRole);

        // Add admin participant for owner
        if (userRole.getRole().equals(ObjectRole.OWNER)) {
            boolean administratorExists = false;
            for (ParticipantModel participant : reservationRequest.getRoomParticipants()) {
                if (ParticipantModel.Type.USER.equals(participant.getType()) &&
                        ParticipantRole.ADMINISTRATOR.equals(participant.getRole()) &&
                        userRole.getUserId().equals(participant.getUserId())) {
                    administratorExists = true;
                }
            }
            if (!administratorExists) {
                reservationRequest.addRoomParticipant(userRole.getUser(), ParticipantRole.ADMINISTRATOR);
            }
        }

        return handleRoles(reservationRequest);
    }

    /**
     * Delete user role with given {@code userRoleId} from given {@code reservationRequest}.
     *
     * @param reservationRequest session attribute to which the {@code userRole} will be added
     * @param userRoleId         of user role to be deleted
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_ROLE_DELETE, method = RequestMethod.GET)
    public ModelAndView handleRoleDelete(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest,
            @PathVariable("roleId") String userRoleId)
    {
        UserRoleModel userRole = reservationRequest.getUserRole(userRoleId);
        if (userRole == null) {
            throw new IllegalArgumentException("User role " + userRoleId + " doesn't exist.");
        }
        reservationRequest.removeUserRole(userRole);

        // Delete admin participant for owner
        if (userRole.getRole().equals(ObjectRole.OWNER)) {
            for (ParticipantModel participant : reservationRequest.getRoomParticipants()) {
                if (ParticipantModel.Type.USER.equals(participant.getType()) &&
                        ParticipantRole.ADMINISTRATOR.equals(participant.getRole()) &&
                        userRole.getUserId().equals(participant.getUserId())) {
                    reservationRequest.deleteParticipant(participant.getId());
                    break;
                }
            }
        }

        return handleRoles(reservationRequest);
    }

    /**
     * Manage participants for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_PARTICIPANTS, method = RequestMethod.GET)
    public ModelAndView handleParticipants(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequestModel)
    {
        WizardView wizardView = getWizardView(Page.ROOM_PARTICIPANTS, "wizardCreateParticipants.jsp");
        wizardView.addObject("createUrl", ClientWebUrl.WIZARD_PARTICIPANT_CREATE);
        wizardView.addObject("modifyUrl", ClientWebUrl.WIZARD_ROOM_PARTICIPANT_MODIFY);
        wizardView.addObject("deleteUrl", ClientWebUrl.WIZARD_ROOM_PARTICIPANT_DELETE);
        return wizardView;
    }

    /**
     * Show form for adding new participant for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PARTICIPANT_CREATE, method = RequestMethod.GET)
    public ModelAndView handleParticipantCreate(
            SecurityToken securityToken,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        return handleParticipantCreate(Page.ROOM_PARTICIPANTS, reservationRequest, securityToken);
    }

    /**
     * Store new {@code participant} to given {@code reservationRequest}.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_PARTICIPANT_CREATE, method = RequestMethod.POST)
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
            return handleParticipantView(Page.ROOM_PARTICIPANTS, reservationRequest, participant);
        }
    }

    /**
     * Show form for modifying existing participant for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_PARTICIPANT_MODIFY, method = RequestMethod.GET)
    public ModelAndView handleParticipantModify(
            @PathVariable("participantId") String participantId,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        return handleParticipantModify(Page.ROOM_PARTICIPANTS, reservationRequest, participantId);
    }

    /**
     * Store changes for existing {@code participant} to given {@code reservationRequest}.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_PARTICIPANT_MODIFY, method = RequestMethod.POST)
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
            return handleParticipantModify(Page.ROOM_PARTICIPANTS, reservationRequest, participant);
        }
    }

    /**
     * Delete existing {@code participant} from given {@code reservationRequest}.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_PARTICIPANT_DELETE, method = RequestMethod.GET)
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
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_CONFIRM, method = RequestMethod.GET)
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
        WizardView wizardView = getWizardView(Page.ROOM_CONFIRM, "wizardCreateConfirm.jsp");
        wizardView.setNextPageUrl(ClientWebUrl.WIZARD_ROOM_CONFIRMED);
        return wizardView;
    }

    /**
     * Create new reservation request and redirect to it's detail.
     *
     * @param reservationRequest to be created
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_CONFIRMED, method = RequestMethod.GET)
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
            authorizationService.createAclEntry(securityToken,
                    new AclEntry(userRole.getUserId(), reservationRequestId, userRole.getRole()));
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
    @ExceptionHandler({HttpSessionRequiredException.class, IllegalStateException.class})
    public Object handleExceptions(Exception exception)
    {
        logger.warn("Redirecting to " + ClientWebUrl.WIZARD_ROOM + ".", exception);
        return "redirect:" + ClientWebUrl.WIZARD_ROOM;
    }

    /**
     * @return {@link WizardView} for reservation request form
     */
    private WizardView getCreateRoomAttributesView()
    {
        WizardView wizardView = getWizardView(Page.ROOM_ATTRIBUTES, "wizardCreateAttributes.jsp");
        wizardView.setNextPageUrl(SUBMIT_RESERVATION_REQUEST);
        wizardView.addAction(SUBMIT_RESERVATION_REQUEST_FINISH,
                "views.button.finish", WizardView.ActionPosition.RIGHT);
        return wizardView;
    }
}
