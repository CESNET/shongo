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
@SessionAttributes({"reservationRequest", "permanentRooms"})
public class WizardCreatePermanentRoomCapacityController extends AbstractWizardController
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
        CREATE_PERMANENT_ROOM_CAPACITY,
        CREATE_PERMANENT_ROOM_CAPACITY_CONFIRM,
    }

    @Override
    protected void initWizardPages(WizardView wizardView, Object currentWizardPageId)
    {
        wizardView.addPage(new WizardPage(
                Page.CREATE_PERMANENT_ROOM_CAPACITY,
                ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY,
                "views.wizard.page.createPermanentRoomCapacity"));
        wizardView.addPage(new WizardPage(
                Page.CREATE_PERMANENT_ROOM_CAPACITY_CONFIRM,
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
    public ModelAndView handleCreatePermanentRoomCapacity(
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
                (ReservationRequestModel) httpSession.getAttribute("reservationRequest");
        if (reservationRequestModel == null || FORCE_NEW.equals(force)) {
            reservationRequestModel = new ReservationRequestModel();
            wizardView.addObject("reservationRequest", reservationRequestModel);
        }
        reservationRequestModel.setSpecificationType(SpecificationType.PERMANENT_ROOM_CAPACITY);
        reservationRequestModel.setPermanentRoomReservationRequestId(permanentRoomId, permanentRooms);

        return wizardView;
    }

    /**
     * @return {@link WizardView} for reservation request form
     */
    private WizardView getCreatePermanentRoomCapacityView()
    {
        WizardView wizardView = getWizardView(Page.CREATE_PERMANENT_ROOM_CAPACITY, "wizardCreateAttributes.jsp");
        wizardView.setNextPageUrl(WizardCreateController.SUBMIT_RESERVATION_REQUEST);
        wizardView.addAction(WizardCreateController.SUBMIT_RESERVATION_REQUEST_FINISH,
                "views.button.finish", WizardView.ActionPosition.RIGHT);
        return wizardView;
    }

    /**
     * Handle validation of attributes..
     *
     * @param reservationRequest to be validated
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY, method = {RequestMethod.POST})
    public Object handleCreatePermanentRoomCapacityProcess(
            UserSession userSession,
            SecurityToken securityToken,
            SessionStatus sessionStatus,
            @RequestParam(value = "finish", required = false) boolean finish,
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequest,
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
     * Show confirmation for creation of a new reservation request.
     *
     * @param reservationRequest to be confirmed
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_CREATE_PERMANENT_ROOM_CAPACITY_CONFIRM, method = RequestMethod.GET)
    public Object handleCreateConfirm(
            UserSession userSession,
            SecurityToken securityToken,
            @ModelAttribute("reservationRequest") ReservationRequestModel reservationRequest,
            BindingResult bindingResult)
    {
        ReservationRequestValidator validator = new ReservationRequestValidator(securityToken, reservationService,
                userSession.getLocale(), userSession.getTimeZone());
        validator.validate(reservationRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            return getCreatePermanentRoomCapacityView();
        }
        reservationRequest.loadPermanentRoom(new CacheProvider(cache, securityToken));
        WizardView wizardView = getWizardView(Page.CREATE_PERMANENT_ROOM_CAPACITY_CONFIRM, "wizardCreateConfirm.jsp");
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
}
