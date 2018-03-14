package cz.cesnet.shongo.client.web.controllers;

import com.google.common.base.Strings;
import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.client.web.*;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.BackUrl;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.client.web.support.editors.*;
import cz.cesnet.shongo.controller.AclIdentityType;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
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
        WizardRoomController.USER_ROLE_ATTRIBUTE
})
public class WizardRoomController extends WizardParticipantsController
{
    private static Logger logger = LoggerFactory.getLogger(WizardRoomController.class);

    protected static final String USER_ROLE_ATTRIBUTE = "userRole";

    public static final String SUBMIT_RESERVATION_REQUEST_FINISH_WITH_CAPACITY =
            WizardController.SUBMIT_RESERVATION_REQUEST_FINISH.replace("finish", "finish-with-capacity");

    @Resource
    private Cache cache;

    @Resource
    private ReservationService reservationService;

    @Resource
    private AuthorizationService authorizationService;

    private static enum Page
    {
        SELECT,
        ATTRIBUTES,
        ROLES,
        PARTICIPANTS,
        CONFIRM
    }

    @Override
    protected void initWizardPages(WizardView wizardView, Object currentWizardPageId, MessageProvider messageProvider)
    {
        ReservationRequestModel reservationRequest =
                (ReservationRequestModel) WebUtils.getSessionAttribute(request, RESERVATION_REQUEST_ATTRIBUTE);

        if (reservationRequest != null && reservationRequest instanceof ReservationRequestModificationModel) {
            wizardView.addPage(new WizardPage(Page.SELECT, null, "views.wizard.page.room.modify"));
        }
        else if (reservationRequest != null && SpecificationType.MEETING_ROOM.equals(reservationRequest.getSpecificationType())) {
            wizardView.addPage(new WizardPage(Page.SELECT, null, "views.wizard.page.meetingRoom.book"));
        } else {
            wizardView.addPage(new WizardPage(Page.SELECT, ClientWebUrl.WIZARD_ROOM,
                    "views.wizard.page.room.create"));
        }

        wizardView.addPage(new WizardPage(Page.ATTRIBUTES, ClientWebUrl.WIZARD_ROOM_ATTRIBUTES,
                "views.wizard.page.attributes"));
        if (reservationRequest == null || reservationRequest.getSpecificationType() == null
                || reservationRequest.getSpecificationType().equals(SpecificationType.PERMANENT_ROOM)) {
            wizardView.addPage(new WizardPage(Page.ROLES, ClientWebUrl.WIZARD_ROOM_ROLES,
                    "views.wizard.page.userRoles"));
        }
        //TODO:MR temporary until model is changed
        if (reservationRequest == null || !SpecificationType.MEETING_ROOM.equals(reservationRequest.getSpecificationType())) {
            wizardView.addPage(new WizardPage(Page.PARTICIPANTS, ClientWebUrl.WIZARD_ROOM_PARTICIPANTS,
                    "views.wizard.page.participants"));
        }
        wizardView.addPage(new WizardPage(Page.CONFIRM, ClientWebUrl.WIZARD_ROOM_CONFIRM,
                "views.wizard.page.confirm"));
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
        binder.registerCustomEditor(DateTimeZone.class, new DateTimeZoneEditor());
        binder.registerCustomEditor(Period.class, new PeriodEditor());
        binder.registerCustomEditor(LocalDate.class, new LocalDateEditor());
        binder.registerCustomEditor(LocalTime.class, new LocalTimeEditor());
    }

    /**
     * Book new room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM, method = RequestMethod.GET)
    public ModelAndView handleRoomType(SecurityToken securityToken)
    {
        WizardView wizardView = getWizardView(Page.SELECT, "wizardRoomType.jsp");
        ReservationRequestModel reservationRequest = createReservationRequest(securityToken);
        wizardView.addObject(RESERVATION_REQUEST_ATTRIBUTE, reservationRequest);
        wizardView.setNextPageUrl(null);
        wizardView.addAction(ClientWebUrl.WIZARD_ROOM_CANCEL,
                "views.button.cancel", WizardView.ActionPosition.LEFT);
        return wizardView;
    }

    /**
     * Change new virtual room to ad-hoc type and show form for editing room attributes.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_ADHOC, method = RequestMethod.GET)
    public String handleAdhocRoom(
            SecurityToken securityToken,
            UserSession userSession,
            @RequestParam(value = "technology", required = false) TechnologyModel technology,
            @RequestParam(value = "participantCount", required = false) Integer participantCount,
            @RequestParam(value = "duration", required = false) Period duration,
            @RequestParam(value = "confirm", required = false) boolean confirm)
    {
        ReservationRequestModel reservationRequest = getReservationRequest();
        if (reservationRequest == null) {
            reservationRequest = createReservationRequest(securityToken);
            synchronized (request) {
                WebUtils.setSessionAttribute(request, RESERVATION_REQUEST_ATTRIBUTE, reservationRequest);
            }
        }
        reservationRequest.setSpecificationType(SpecificationType.ADHOC_ROOM);
        reservationRequest.initByUserSettings(userSession.getUserSettings());
        if (technology != null) {
            reservationRequest.setTechnology(technology);
        }
        if (participantCount != null) {
            reservationRequest.setRoomParticipantCount(participantCount);
        }
        if (duration != null) {
            reservationRequest.setDuration(duration);
        }
        if (confirm) {
            addDefaultParticipant(securityToken, reservationRequest);
            return "redirect:" + BackUrl.getInstance(request).applyToUrl(ClientWebUrl.WIZARD_ROOM_CONFIRM);
        }
        else {
            return "redirect:" + BackUrl.getInstance(request).applyToUrl(ClientWebUrl.WIZARD_ROOM_ATTRIBUTES);
        }
    }

    /**
     * Change new virtual room to permanent type and show form for editing room attributes.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_PERMANENT, method = RequestMethod.GET)
    public String handlePermanentRoom(SecurityToken securityToken, UserSession userSession)
    {
        ReservationRequestModel reservationRequest = getReservationRequest();
        if (reservationRequest == null) {
            reservationRequest = createReservationRequest(securityToken);
            synchronized (request) {
                WebUtils.setSessionAttribute(request, RESERVATION_REQUEST_ATTRIBUTE, reservationRequest);
            }
        }
        reservationRequest.setSpecificationType(SpecificationType.PERMANENT_ROOM);
        return "redirect:" + BackUrl.getInstance(request).applyToUrl(ClientWebUrl.WIZARD_ROOM_ATTRIBUTES);
    }

    /**
     * TODO:Change new room to meeting type and show form for editing room attributes.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_MEETING_ROOM_BOOK, method = RequestMethod.GET)
    public String handleMeetingRoom(
            SecurityToken securityToken,
            UserSession userSession,
            @RequestParam(value = "start", required = false) DateTime start,
            @RequestParam(value = "end", required = false) DateTime end,
            @RequestParam(value = "resourceId", required = false) String meetingRoomResourceId
            )
    {
        ReservationRequestModel reservationRequest = getReservationRequest();
        if (reservationRequest == null) {
            reservationRequest = createReservationRequest(securityToken);
            synchronized (request) {
                WebUtils.setSessionAttribute(request, RESERVATION_REQUEST_ATTRIBUTE, reservationRequest);
            }
        }

        reservationRequest.setSpecificationType(SpecificationType.MEETING_ROOM);

        if (start != null) {
            reservationRequest.setStart(start.toLocalTime());
            reservationRequest.setStartDate(start.toLocalDate());
        }

        if (end != null) {
            reservationRequest.setEnd(end);
        }

        if (start != null && end != null) {
            reservationRequest.setDuration(Temporal.getIntervalDuration(new Interval(start,end)));
        }

        if (meetingRoomResourceId != null) {
            reservationRequest.setMeetingRoomResourceId(meetingRoomResourceId);
        }
       return "redirect:" + BackUrl.getInstance(request).applyToUrl(ClientWebUrl.WIZARD_MEETING_ROOM_ATTRIBUTES);
    }

    /**
     * TODO:Change new room to meeting type and show form for editing room attributes.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_MEETING_ROOM_BOOK, method = RequestMethod.POST)
    public Object handleMeetingRoomConfirm(
            SecurityToken securityToken,
            SessionStatus sessionStatus,
            @RequestParam(value = "start") DateTime start,
            @RequestParam(value = "end") DateTime end,
            @RequestParam(value = "resourceId") String meetingRoomResourceId,
            @RequestParam(value = "description", required = false) String description)
    {
        ReservationRequestModel reservationRequest = getReservationRequest();
        if (reservationRequest == null) {
            reservationRequest = createReservationRequest(securityToken);
            synchronized (request) {
                WebUtils.setSessionAttribute(request, RESERVATION_REQUEST_ATTRIBUTE, reservationRequest);
            }
        }

        reservationRequest.setSpecificationType(SpecificationType.MEETING_ROOM);

        if (start != null) {
            reservationRequest.setStart(start.toLocalTime());
            reservationRequest.setStartDate(start.toLocalDate());
        }

        if (end != null) {
            reservationRequest.setEnd(end);
        }

        if (start != null && end != null) {
            reservationRequest.setDuration(Temporal.getIntervalDuration(new Interval(start, end)));
        }

        if (meetingRoomResourceId != null) {
            reservationRequest.setMeetingRoomResourceId(meetingRoomResourceId);
        }

        if (!Strings.isNullOrEmpty(description)) {
            reservationRequest.setDescription(description);
        }

        addDefaultParticipant(securityToken, reservationRequest);

        return handleConfirmed(securityToken, sessionStatus, reservationRequest);
    }

    /**
     * Handle duplication of an existing reservation request.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_DUPLICATE, method = RequestMethod.GET)
    public String handleRoomDuplicate(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        AbstractReservationRequest reservationRequest =
                reservationService.getReservationRequest(securityToken, reservationRequestId);
        ReservationRequestModel reservationRequestModel =
                new ReservationRequestModel(reservationRequest, new CacheProvider(cache, securityToken));
        reservationRequestModel.setId(null);
        reservationRequestModel.setStart(LocalTime.now());
        reservationRequestModel.setStartDate(LocalDate.now());
        synchronized (request) {
            WebUtils.setSessionAttribute(request, RESERVATION_REQUEST_ATTRIBUTE, reservationRequestModel);
        }
        return "redirect:" + BackUrl.getInstance(request).applyToUrl(ClientWebUrl.WIZARD_ROOM_ATTRIBUTES);
    }

    /**
     * Modify existing virtual room.
     */
    @RequestMapping(value = {ClientWebUrl.WIZARD_ROOM_MODIFY, ClientWebUrl.WIZARD_MEETING_ROOM_MODIFY}, method = RequestMethod.GET)
    public String handleRoomModify(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        AbstractReservationRequest reservationRequest =
                reservationService.getReservationRequest(securityToken, reservationRequestId);
        ReservationRequestModel reservationRequestModel = new ReservationRequestModificationModel(
                reservationRequest, new CacheProvider(cache, securityToken), authorizationService);
        synchronized (request) {
            WebUtils.setSessionAttribute(request, RESERVATION_REQUEST_ATTRIBUTE, reservationRequestModel);
        }
        return "redirect:" + BackUrl.getInstance(request).applyToUrl(ClientWebUrl.WIZARD_ROOM_ATTRIBUTES);
    }

    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_PERIODIC_REMOVE, method = RequestMethod.GET)
    public Object handlePeriodicRoomExclude(
            UserSession userSession,
            SecurityToken securityToken,
            SessionStatus sessionStatus,
            @PathVariable(value = "reservationRequestId") String reservationRequestId,
            @RequestParam(value = "excludeReservationId") String excludeReservationId)
    {
        AbstractReservationRequest reservationRequest =
                reservationService.getReservationRequest(securityToken, reservationRequestId);
        if (!(reservationRequest instanceof ReservationRequestSet) || Strings.isNullOrEmpty(excludeReservationId)) {
            throw new IllegalArgumentException("Invalid reservation request or reservation id not provided.");
        }
        Reservation excludeReservation = reservationService.getReservation(securityToken, excludeReservationId);
        LocalDate excludeDate = excludeReservation.getSlot().getStart().toLocalDate();

        ReservationRequestSet reservationRequestSet = (ReservationRequestSet) reservationRequest;
        for (Object slot : reservationRequestSet.getSlots()) {
            if (slot instanceof PeriodicDateTimeSlot) {
                PeriodicDateTimeSlot periodicDateTimeSlot = (PeriodicDateTimeSlot) slot;
                periodicDateTimeSlot.addExcludeDate(excludeDate);
            }
        }
        ReservationRequestModel reservationRequestModel = new ReservationRequestModificationModel(
                reservationRequest, new CacheProvider(cache, securityToken), authorizationService);
        reservationRequestModel.setRemovedReservationDate(excludeDate);
        synchronized (request) {
            WebUtils.setSessionAttribute(request, RESERVATION_REQUEST_ATTRIBUTE, reservationRequestModel);
        }

        // Init of DataBinder for binding results (normally done by Spring MVC)
        DataBinder dataBinder = new DataBinder(reservationRequestModel);
        dataBinder.setConversionService(new DefaultFormattingConversionService());
        BindingResult bindingResult = dataBinder.getBindingResult();

        return handleRoomAttributesProcess(userSession, securityToken, sessionStatus, false, false, reservationRequestModel, bindingResult);
    }

    /**
     * Show form for editing ad-hoc/permanent/meeting room attributes.
     *
     * @param reservationRequest session attribute is required
     */
    @RequestMapping(value = {ClientWebUrl.WIZARD_ROOM_ATTRIBUTES, ClientWebUrl.WIZARD_MEETING_ROOM_ATTRIBUTES}, method = RequestMethod.GET)
    public ModelAndView handleRoomAttributes(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        return getCreateRoomAttributesView(reservationRequest);
    }

    /**
     * Handle validation of attributes..
     * For periodic request updates startDate to first future slot.
     *
     * @param reservationRequest to be validated
     */
    @RequestMapping(value = {ClientWebUrl.WIZARD_ROOM_ATTRIBUTES, ClientWebUrl.WIZARD_MEETING_ROOM_ATTRIBUTES}, method = {RequestMethod.POST})
    public Object handleRoomAttributesProcess(
            UserSession userSession,
            SecurityToken securityToken,
            SessionStatus sessionStatus,
            @RequestParam(value = "finish", required = false) boolean finish,
            @RequestParam(value = "finish-with-capacity", required = false) boolean finishWithCapacity,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest,
            BindingResult bindingResult)
    {
        //on FreePBX we always reserve whole room not just the capacity so room = participant
        if (reservationRequest.getTechnology() != null) {
            if (reservationRequest.getTechnology().equals(TechnologyModel.FREEPBX)){
                reservationRequest.setRoomParticipantCount(1);
            }
        }
        // Set valid startDate to be in the future
        reservationRequest.updateSlotStartToFutureSlot();

        ReservationRequestValidator validator = new ReservationRequestValidator(
                securityToken, reservationService, cache, userSession.getLocale(), userSession.getTimeZone());
        validator.validate(reservationRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            CommonModel.logValidationErrors(logger, bindingResult, securityToken);
            // Skip error if colliding interval is for future periodic reservation request
            if (bindingResult.getFieldError("collidingInterval") == null) {
                return getCreateRoomAttributesView(reservationRequest);
            }
        }
        addDefaultParticipant(securityToken, reservationRequest);
        if (finishWithCapacity) {
            handleConfirmed(securityToken, sessionStatus, reservationRequest);
            return createPermanentRoomCapacity(securityToken, reservationRequest.getId());
        }
        else if (finish) {
            if (bindingResult.getFieldError("collidingInterval") == null) {
                return handleConfirmed(securityToken, sessionStatus, reservationRequest);
            }
        }
        if (SpecificationType.PERMANENT_ROOM.equals(reservationRequest.getSpecificationType())) {
            return "redirect:" + ClientWebUrl.WIZARD_ROOM_ROLES;
        }
        //TODO MR: temporary until model change
        else if (SpecificationType.MEETING_ROOM.equals(reservationRequest.getSpecificationType())) {
            return "redirect:" + ClientWebUrl.WIZARD_ROOM_CONFIRM;
        }
        else {
            return "redirect:" + ClientWebUrl.WIZARD_ROOM_PARTICIPANTS;
        }
    }

    /**
     * Manage user roles for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_ROLES, method = RequestMethod.GET)
    public ModelAndView handleRoles(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        WizardView wizardView = getWizardView(Page.ROLES, "wizardRoomRoles.jsp");
        wizardView.addAction(ClientWebUrl.WIZARD_ROOM_FINISH,
                "views.button.finish", WizardView.ActionPosition.RIGHT);
        if (SpecificationType.PERMANENT_ROOM.equals(reservationRequest.getSpecificationType()) &&
                reservationRequest.getId() == null) {
            wizardView.addAction(ClientWebUrl.WIZARD_ROOM_FINISH_WITH_CAPACITY,
                    "views.wizard.finishWithCapacity", WizardView.ActionPosition.RIGHT);
        }
        return wizardView;
    }

    /**
     * Show form for adding new user role for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_ROLE_CREATE, method = RequestMethod.GET)
    public ModelAndView handleRoleCreate(
            SecurityToken securityToken,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        WizardView wizardView = getWizardView(Page.ROLES, "wizardRoomRole.jsp");
        CacheProvider cacheProvider = new CacheProvider(cache, securityToken);
        wizardView.addObject(WizardRoomController.USER_ROLE_ATTRIBUTE, new UserRoleModel(cacheProvider));
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
    public Object handleRoleCreateProcess(
            HttpSession httpSession,
            SecurityToken securityToken,
            @ModelAttribute(WizardRoomController.USER_ROLE_ATTRIBUTE) UserRoleModel userRole,
            BindingResult bindingResult)
    {
        UserRoleValidator userRoleValidator = new UserRoleValidator();
        userRoleValidator.validate(userRole, bindingResult);
        if (bindingResult.hasErrors()) {
            CommonModel.logValidationErrors(logger, bindingResult, securityToken);

            // Show form for adding new user role with validation errors
            WizardView wizardView = getWizardView(Page.ROLES, "wizardRoomRole.jsp");
            wizardView.setNextPageUrl(null);
            wizardView.setPreviousPageUrl(null);
            return wizardView;
        }
        userRole.setNewId();
        userRole.setDeletable(true);
        ReservationRequestModel reservationRequest = getReservationRequest(httpSession);
        reservationRequest.addUserRole(userRole);

        // Add admin participant for owner
        if (userRole.getIdentityType().equals(AclIdentityType.USER) && userRole.getRole().equals(ObjectRole.OWNER)) {
            ParticipantRole defaultParticipantRole = reservationRequest.getDefaultOwnerParticipantRole();
            boolean defaultParticipantRoleExists = false;
            for (ParticipantModel participant : reservationRequest.getRoomParticipants()) {
                if (ParticipantModel.Type.USER.equals(participant.getType()) &&
                        defaultParticipantRole.equals(participant.getRole()) &&
                        userRole.getIdentityPrincipalId().equals(participant.getUserId())) {
                    defaultParticipantRoleExists = true;
                }
            }
            if (!defaultParticipantRoleExists) {
                reservationRequest.addRoomParticipant(userRole.getUser(), defaultParticipantRole);
            }
        }

        return "redirect:" + ClientWebUrl.WIZARD_ROOM_ROLES;
    }

    /**
     * Delete user role with given {@code userRoleId} from given {@code reservationRequest}.
     *
     * @param reservationRequest session attribute to which the {@code userRole} will be added
     * @param userRoleId         of user role to be deleted
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_ROLE_DELETE, method = RequestMethod.GET)
    public Object handleRoleDelete(
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
            ParticipantRole defaultParticipantRole = reservationRequest.getDefaultOwnerParticipantRole();
            for (ParticipantModel participant : reservationRequest.getRoomParticipants()) {
                if (ParticipantModel.Type.USER.equals(participant.getType()) &&
                        defaultParticipantRole.equals(participant.getRole()) &&
                        userRole.getIdentityPrincipalId().equals(participant.getUserId())) {
                    reservationRequest.deleteParticipant(participant.getId());
                    break;
                }
            }
        }

        return "redirect:" + ClientWebUrl.WIZARD_ROOM_ROLES;
    }

    /**
     * Manage participants for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_PARTICIPANTS, method = RequestMethod.GET)
    public ModelAndView handleParticipants(
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        WizardView wizardView = getWizardView(Page.PARTICIPANTS, "wizardRoomParticipants.jsp");
        wizardView.addObject("createUrl", ClientWebUrl.WIZARD_ROOM_PARTICIPANT_CREATE);
        wizardView.addObject("modifyUrl", ClientWebUrl.WIZARD_ROOM_PARTICIPANT_MODIFY);
        wizardView.addObject("deleteUrl", ClientWebUrl.WIZARD_ROOM_PARTICIPANT_DELETE);
        wizardView.setNextPageUrl(WizardController.SUBMIT_RESERVATION_REQUEST);
        wizardView.addAction(WizardController.SUBMIT_RESERVATION_REQUEST_FINISH,
                "views.button.finish", WizardView.ActionPosition.RIGHT);
        if (SpecificationType.PERMANENT_ROOM.equals(reservationRequest.getSpecificationType()) &&
                reservationRequest.getId() == null) {
            wizardView.addAction(SUBMIT_RESERVATION_REQUEST_FINISH_WITH_CAPACITY,
                    "views.wizard.finishWithCapacity", WizardView.ActionPosition.RIGHT);
        }
        return wizardView;
    }

    /**
     * Process participants form.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_PARTICIPANTS, method = RequestMethod.POST)
    public Object handleParticipantsProcess(

            @RequestParam(value = "finish", required = false) boolean finish,
            @RequestParam(value = "finish-with-capacity", required = false) boolean finishWithCapacity,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequestModel,
            BindingResult bindingResult)
    {
        if (!ReservationRequestValidator.validateParticipants(reservationRequestModel, bindingResult, false)) {
            return handleParticipants(reservationRequestModel);
        }
        if (finishWithCapacity) {
            return "redirect:" + ClientWebUrl.WIZARD_ROOM_FINISH_WITH_CAPACITY;
        }
        else if (finish) {
            return "redirect:" + ClientWebUrl.WIZARD_ROOM_FINISH;
        }
        else {
            return "redirect:" + ClientWebUrl.WIZARD_ROOM_CONFIRM;
        }
    }

    /**
     * Show form for adding new participant for ad-hoc/permanent room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_PARTICIPANT_CREATE, method = RequestMethod.GET)
    public ModelAndView handleParticipantCreate(
            SecurityToken securityToken,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        return handleParticipantCreate(Page.PARTICIPANTS, reservationRequest, securityToken);
    }

    /**
     * Store new {@code participant} to given {@code reservationRequest}.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_PARTICIPANT_CREATE, method = RequestMethod.POST)
    public Object handleParticipantCreateProcess(
            HttpSession httpSession,
            SecurityToken securityToken,
            @ModelAttribute(PARTICIPANT_ATTRIBUTE) ParticipantModel participant,
            BindingResult bindingResult)
    {
        ReservationRequestModel reservationRequest = getReservationRequest(httpSession);
        if (reservationRequest.createParticipant(participant, bindingResult, securityToken)) {
            return "redirect:" + ClientWebUrl.WIZARD_ROOM_PARTICIPANTS;
        }
        else {
            return handleParticipantView(Page.PARTICIPANTS, reservationRequest, participant);
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
        return handleParticipantModify(Page.PARTICIPANTS, reservationRequest, participantId);
    }

    /**
     * Store changes for existing {@code participant} to given {@code reservationRequest}.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_PARTICIPANT_MODIFY, method = RequestMethod.POST)
    public Object handleParticipantModifyProcess(
            HttpSession httpSession,
            SecurityToken securityToken,
            @PathVariable("participantId") String participantId,
            @ModelAttribute(PARTICIPANT_ATTRIBUTE) ParticipantModel participant,
            BindingResult bindingResult)
    {
        ReservationRequestModel reservationRequest = getReservationRequest(httpSession);
        if (reservationRequest.modifyParticipant(participantId, participant, bindingResult, securityToken)) {
            return "redirect:" + ClientWebUrl.WIZARD_ROOM_PARTICIPANTS;
        }
        else {
            return handleParticipantModify(Page.PARTICIPANTS, reservationRequest, participant);
        }
    }

    /**
     * Delete existing {@code participant} from given {@code reservationRequest}.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_PARTICIPANT_DELETE, method = RequestMethod.GET)
    public Object handleParticipantDelete(
            @PathVariable("participantId") String participantId,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest)
    {
        reservationRequest.deleteParticipant(participantId);
        return "redirect:" + ClientWebUrl.WIZARD_ROOM_PARTICIPANTS;
    }

    /**
     * Show confirmation for creation of a new reservation request.
     *
     * @param reservationRequest to be confirmed
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_CONFIRM)
    public Object handleConfirm(
            UserSession userSession,
            SecurityToken securityToken,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest,
            BindingResult bindingResult)
    {
        ReservationRequestValidator validator = new ReservationRequestValidator(
                securityToken, reservationService, cache, userSession.getLocale(), userSession.getTimeZone());
        validator.validate(reservationRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            CommonModel.logValidationErrors(logger, bindingResult, securityToken);
            // If there is no colliding interval with first slot return error otherwise set collidingInterval to be shown
            if (bindingResult.getFieldError("collidingInterval") == null) {
                return getCreateRoomAttributesView(reservationRequest);
            } else {
                reservationRequest.setCollidingInterval(new Interval(bindingResult.getFieldError("collidingInterval").getDefaultMessage()));
            }
        }
        else {
            reservationRequest.resetCollidingInterval();
        }
        WizardView wizardView = getWizardView(Page.CONFIRM, "wizardRoomConfirm.jsp");
        wizardView.setNextPageUrl(ClientWebUrl.WIZARD_ROOM_CONFIRMED);
        if (SpecificationType.PERMANENT_ROOM.equals(reservationRequest.getSpecificationType()) &&
                reservationRequest.getId() == null) {
            wizardView.addAction(ClientWebUrl.WIZARD_ROOM_FINISH_WITH_CAPACITY,
                    "views.wizard.finishWithCapacity", WizardView.ActionPosition.RIGHT);
        }
        wizardView.addAction(ClientWebUrl.WIZARD_ROOM_CANCEL,
                "views.button.cancel", WizardView.ActionPosition.RIGHT);
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
        // Create or modify reservation request
        String reservationRequestId;
        if (Strings.isNullOrEmpty(reservationRequest.getId())) {
            reservationRequestId = reservationService.createReservationRequest(
                    securityToken, reservationRequest.toApi(request));
        }
        else {
            reservationRequestId = reservationService.modifyReservationRequest(
                    securityToken, reservationRequest.toApi(request));
        }
        reservationRequest.setId(reservationRequestId);
        if (!reservationRequest.getSpecificationType().equals(SpecificationType.PERMANENT_ROOM)) {
            UserSettingsModel.updateSlotSettings(securityToken, reservationRequest, request, authorizationService);
        }

        // Create user roles
        for (UserRoleModel userRole : reservationRequest.getUserRoles()) {
            if (!userRole.isNew()) {
                continue;
            }
            userRole.setObjectId(reservationRequestId);
            authorizationService.createAclEntry(securityToken, userRole.toApi());
        }

        // Clear session attributes
        sessionStatus.setComplete();

        // Show detail of newly created reservation request
        return "redirect:" + BackUrl.getInstance(request, ClientWebUrl.WIZARD_ROOM_ATTRIBUTES).applyToUrl(
                ClientWebUrl.format(ClientWebUrl.DETAIL_VIEW, reservationRequestId));
    }

    /**
     * Cancel the reservation request.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_CANCEL, method = RequestMethod.GET)
    public Object handleCancel(
            SessionStatus sessionStatus)
    {
        sessionStatus.setComplete();
        BackUrl backUrl = BackUrl.getInstance(request, ClientWebUrl.WIZARD_ROOM_ATTRIBUTES);
        return "redirect:" + backUrl.getUrl(ClientWebUrl.HOME);
    }

    /**
     * Finish the reservation request.
     *
     * @param reservationRequest to be finished
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_ROOM_FINISH, method = RequestMethod.GET)
    public Object handleFinish(
            SecurityToken securityToken,
            UserSession userSession,
            SessionStatus sessionStatus,
            @RequestParam(value = "create-capacity", required = false) boolean createCapacity,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest,
            BindingResult bindingResult)
    {
        ReservationRequestValidator validator = new ReservationRequestValidator(
                securityToken, reservationService, cache, userSession.getLocale(), userSession.getTimeZone());
        validator.validate(reservationRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            CommonModel.logValidationErrors(logger, bindingResult, securityToken);
            return getCreateRoomAttributesView(reservationRequest);
        }
        Object result = handleConfirmed(securityToken, sessionStatus, reservationRequest);
        if (createCapacity) {
            return createPermanentRoomCapacity(securityToken, reservationRequest.getId());
        }
        else {
            return result;
        }
    }

    private Object createPermanentRoomCapacity(SecurityToken securityToken, String reservationRequestId)
    {
        AllocationState allocationState = null;
        for (int index = 0; index < 5; index++) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException exception) {
                logger.warn("Sleep failed", exception);
            }
            ReservationRequest reservationRequest =
                    (ReservationRequest) reservationService.getReservationRequest(securityToken, reservationRequestId);
            allocationState = reservationRequest.getAllocationState();
            if (!allocationState.equals(AllocationState.NOT_ALLOCATED)) {
                break;
            }
        }
        if (AllocationState.ALLOCATED.equals(allocationState)) {
            // Create permanent room capacity
            return "redirect:" + BackUrl.getInstance(request, ClientWebUrl.WIZARD_ROOM_ATTRIBUTES).applyToUrl(
                    ClientWebUrl.WIZARD_PERMANENT_ROOM_CAPACITY + "?permanentRoom=" + reservationRequestId);
        }
        else {
            // Show detail of reservation request
            return "redirect:" + BackUrl.getInstance(request, ClientWebUrl.WIZARD_ROOM_ATTRIBUTES).applyToUrl(
                    ClientWebUrl.format(ClientWebUrl.DETAIL_VIEW, reservationRequestId));
        }
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
    private WizardView getCreateRoomAttributesView(ReservationRequestModel reservationRequest)
    {
        if (reservationRequest.getSpecificationType() == null) {
            throw new IllegalStateException("Room type is not specified.");
        }
        WizardView wizardView = getWizardView(Page.ATTRIBUTES, "wizardRoomAttributes.jsp");
        wizardView.setNextPageUrl(WizardController.SUBMIT_RESERVATION_REQUEST);
        wizardView.addAction(ClientWebUrl.WIZARD_ROOM_CANCEL,
                "views.button.cancel", WizardView.ActionPosition.LEFT);
        wizardView.addAction(WizardController.SUBMIT_RESERVATION_REQUEST_FINISH,
                "views.button.finish", WizardView.ActionPosition.RIGHT);
        if (SpecificationType.PERMANENT_ROOM.equals(reservationRequest.getSpecificationType()) &&
                reservationRequest.getId() == null) {
            wizardView.addAction(SUBMIT_RESERVATION_REQUEST_FINISH_WITH_CAPACITY,
                    "views.wizard.finishWithCapacity", WizardView.ActionPosition.RIGHT);
        }
        return wizardView;
    }

    /**
     * @return {@link ReservationRequestModel} from {@link #request}
     */
    private ReservationRequestModel getReservationRequest()
    {
        Object reservationRequestAttribute = WebUtils.getSessionAttribute(request, RESERVATION_REQUEST_ATTRIBUTE);
        if (reservationRequestAttribute instanceof ReservationRequestModel) {
            return (ReservationRequestModel) reservationRequestAttribute;
        }
        else {
            return null;
        }
    }

    /**
     * @return {@link ReservationRequestModel} from {@link #request}
     */
    private ReservationRequestModel createReservationRequest(SecurityToken securityToken)
    {
        ReservationRequestModel reservationRequest = new ReservationRequestModel(
                new CacheProvider(cache, securityToken));
        reservationRequest.addUserRole(securityToken.getUserInformation(), ObjectRole.OWNER);
        return reservationRequest;
    }

    /**
     * @param securityToken
     * @param reservationRequest to which a default participant should be added
     */
    private void addDefaultParticipant(SecurityToken securityToken, ReservationRequestModel reservationRequest)
    {
        if (reservationRequest.getRoomParticipants().size() == 0) {
            ParticipantRole participantRole = reservationRequest.getDefaultOwnerParticipantRole();
            if (!reservationRequest.hasUserParticipant(securityToken.getUserId(), participantRole)) {
                reservationRequest.addRoomParticipant(securityToken.getUserInformation(), participantRole);
            }
        }
    }
}
