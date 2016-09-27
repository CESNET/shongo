package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.*;
import cz.cesnet.shongo.client.web.models.*;
import cz.cesnet.shongo.client.web.support.BackUrl;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.client.web.support.editors.*;
import cz.cesnet.shongo.controller.api.AbstractReservationRequest;
import cz.cesnet.shongo.controller.api.SecurityToken;
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

import javax.annotation.Resource;

/**
 * Controller for common wizard actions.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
@SessionAttributes({
        WizardParticipantsController.RESERVATION_REQUEST_ATTRIBUTE
})
public class WizardModifyController extends AbstractWizardController
{
    private static Logger logger = LoggerFactory.getLogger(WizardModifyController.class);

    @Resource
    private Cache cache;

    @Resource
    private ReservationService reservationService;

    @Resource
    private AuthorizationService authorizationService;

    private static enum Page
    {
        EXTEND,
        ENLARGE,
        RECORDED
    }

    @Override
    protected void initWizardPages(WizardView wizardView, Object currentWizardPageId, MessageProvider messageProvider)
    {
        SpecificationType specificationType = SpecificationType.ADHOC_ROOM;
        String titleArgument = specificationType.getForMessage(messageProvider);
        Page page = (Page) currentWizardPageId;
        switch (page) {
            case EXTEND:
                wizardView.addPage(new WizardPage(page, null, "views.wizard.page.modifyExtend", titleArgument));
                break;
            case ENLARGE:
                wizardView.addPage(new WizardPage(page, null, "views.wizard.page.modifyEnlarge", titleArgument));
                break;
            case RECORDED:
                wizardView.addPage(new WizardPage(page, null, "views.wizard.page.modifyRecorded", titleArgument));
                break;
        }
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
    }

    /**
     * Extend existing virtual room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_MODIFY_EXTEND, method = RequestMethod.GET)
    public ModelAndView handleExtend(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        // Create reservation request model
        AbstractReservationRequest reservationRequest =
                reservationService.getReservationRequest(securityToken, reservationRequestId);
        ReservationRequestModel reservationRequestModel = new ReservationRequestModificationModel(
                reservationRequest, new CacheProvider(cache, securityToken), authorizationService);
        reservationRequestModel.setEnd(reservationRequestModel.getEnd().plusMinutes(60));

        // Copy back-url to WIZARD_MODIFY (to be used in WIZARD_MODIFY_CANCEL)
        BackUrl.applyTo(request, ClientWebUrl.format(ClientWebUrl.WIZARD_MODIFY, reservationRequestId));

        // Return view
        return getView(Page.EXTEND, "wizardModifyExtend.jsp", reservationRequestModel);
    }

    /**
     * Process result from {@link #handleExtend}
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_MODIFY_EXTEND, method = RequestMethod.POST)
    public Object handleExtendProcess(
            UserSession userSession,
            SecurityToken securityToken,
            SessionStatus sessionStatus,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModificationModel reservationRequest,
            BindingResult bindingResult)
    {
        String reservationRequestId = reservationRequest.getId();
        if (reservationRequest.isEndChanged()) {
            // Duration should be computed from end field
            reservationRequest.setDurationCount(null);
            reservationRequest.setDurationType(null);

            // Validate reservation request
            ReservationRequestValidator validator = new ReservationRequestValidator(
                    securityToken, reservationService, cache, userSession.getLocale(), userSession.getTimeZone());
            validator.validate(reservationRequest, bindingResult);
            if (bindingResult.hasErrors()) {
                CommonModel.logValidationErrors(logger, bindingResult, securityToken);
                return getView(Page.EXTEND, "wizardModifyExtend.jsp", reservationRequest);
            }

            // Modify reservation request
            reservationRequestId = reservationService.modifyReservationRequest(
                    securityToken, reservationRequest.toApi(request));
        }

        // Clear session attributes
        sessionStatus.setComplete();

        // Redirect to detail
        return "redirect:" + BackUrl.getInstance(request, ClientWebUrl.WIZARD_MODIFY).applyToUrl(
                ClientWebUrl.format(ClientWebUrl.DETAIL_VIEW, reservationRequestId));
    }

    /**
     * Enlarge existing virtual room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_MODIFY_ENLARGE, method = RequestMethod.GET)
    public ModelAndView handleEnlarge(
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        // Create reservation request model
        AbstractReservationRequest reservationRequest =
                reservationService.getReservationRequest(securityToken, reservationRequestId);
        ReservationRequestModel reservationRequestModel = new ReservationRequestModificationModel(
                reservationRequest, new CacheProvider(cache, securityToken), authorizationService);
        reservationRequestModel.setRoomParticipantCount(reservationRequestModel.getRoomParticipantCount() + 1);

        // Copy back-url to WIZARD_MODIFY (to be used in WIZARD_MODIFY_CANCEL)
        BackUrl.applyTo(request, ClientWebUrl.format(ClientWebUrl.WIZARD_MODIFY, reservationRequestId));

        // Return view
        return getView(Page.ENLARGE, "wizardModifyEnlarge.jsp", reservationRequestModel);
    }

    /**
     * Process result from {@link #handleEnlarge}
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_MODIFY_ENLARGE, method = RequestMethod.POST)
    public Object handleEnlargeProcess(
            UserSession userSession,
            SecurityToken securityToken,
            SessionStatus sessionStatus,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModificationModel reservationRequest,
            BindingResult bindingResult)
    {
        String reservationRequestId = reservationRequest.getId();
        if (reservationRequest.isRoomParticipantCountChanged()) {
            // Validate reservation request
            ReservationRequestValidator validator = new ReservationRequestValidator(
                    securityToken, reservationService, cache, userSession.getLocale(), userSession.getTimeZone());
            validator.validate(reservationRequest, bindingResult);
            if (bindingResult.hasErrors()) {
                CommonModel.logValidationErrors(logger, bindingResult, securityToken);
                return getView(Page.ENLARGE, "wizardModifyEnlarge.jsp", reservationRequest);
            }

            // Modify reservation request
            reservationRequestId = reservationService.modifyReservationRequest(
                    securityToken, reservationRequest.toApi(request));
        }

        // Clear session attributes
        sessionStatus.setComplete();

        // Redirect to detail
        return "redirect:" + BackUrl.getInstance(request, ClientWebUrl.WIZARD_MODIFY).applyToUrl(
                ClientWebUrl.format(ClientWebUrl.DETAIL_VIEW, reservationRequestId));
    }

    /**
     * Enlarge existing virtual room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_MODIFY_RECORDED, method = RequestMethod.GET)
    public ModelAndView handleRecorded(
            UserSession userSession,
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        // Create reservation request model
        AbstractReservationRequest reservationRequest =
                reservationService.getReservationRequest(securityToken, reservationRequestId);
        ReservationRequestModel reservationRequestModel = new ReservationRequestModificationModel(
                reservationRequest, new CacheProvider(cache, securityToken), authorizationService);

        // Check whether room already isn't requested to be recorded
        if (reservationRequestModel.isRoomRecorded()) {
            throw new UserMessageException("error.room.alreadyRecorded");
        }
        // Set room as recorded
        reservationRequestModel.setRoomRecorded(true);

        // Copy back-url to WIZARD_MODIFY (to be used in WIZARD_MODIFY_CANCEL)
        BackUrl.applyTo(request, ClientWebUrl.format(ClientWebUrl.WIZARD_MODIFY, reservationRequestId));

        // Return view
        WizardView wizardView = getView(Page.RECORDED, "wizardModifyRecorded.jsp", reservationRequestModel);

        // Validate recorded
        // Init of DataBinder for binding results (normally done by Spring MVC)
        DataBinder dataBinder = new DataBinder(reservationRequestModel);
        dataBinder.setConversionService(new DefaultFormattingConversionService());
        BindingResult bindingResult = dataBinder.getBindingResult();

        ReservationRequestValidator validator = new ReservationRequestValidator(
                securityToken, reservationService, cache, userSession.getLocale(), userSession.getTimeZone());
        validator.validate(reservationRequestModel, bindingResult);
        if (bindingResult.hasErrors()) {
            wizardView.addObject("errors", bindingResult);
            wizardView.removeAction(WizardController.SUBMIT_RESERVATION_REQUEST_FINISH);
        }
        return wizardView;
    }

    /**
     * Process result from {@link #handleRecorded}
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_MODIFY_RECORDED, method = RequestMethod.POST)
    public Object handleRecordedProcess(
            UserSession userSession,
            SecurityToken securityToken,
            SessionStatus sessionStatus,
            @ModelAttribute(RESERVATION_REQUEST_ATTRIBUTE) ReservationRequestModel reservationRequest,
            BindingResult bindingResult)
    {
        // Validate recorded
        ReservationRequestValidator validator = new ReservationRequestValidator(
                securityToken, reservationService, cache, userSession.getLocale(), userSession.getTimeZone());
        validator.validate(reservationRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            WizardView wizardView = getView(Page.RECORDED, "wizardModifyRecorded.jsp", reservationRequest);
            wizardView.addObject("errors", bindingResult);
            return wizardView;
        }

        // Modify reservation request
        String reservationRequestId = reservationService.modifyReservationRequest(
                securityToken, reservationRequest.toApi(request));

        // Clear session attributes
        sessionStatus.setComplete();

        // Redirect to detail
        return "redirect:" + BackUrl.getInstance(request, ClientWebUrl.WIZARD_MODIFY).applyToUrl(
                ClientWebUrl.format(ClientWebUrl.DETAIL_VIEW, reservationRequestId));
    }

    /**
     * Cancel the modification.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_MODIFY_CANCEL, method = RequestMethod.GET)
    public Object handleCancel(
            SessionStatus sessionStatus,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        // Clear session attributes
        sessionStatus.setComplete();

        // Get back-url which is stored for WIZARD_MODIFY
        BackUrl backUrl = BackUrl.getInstance(request,
                ClientWebUrl.format(ClientWebUrl.WIZARD_MODIFY, reservationRequestId));

        // Redirect to back-url
        return "redirect:" + backUrl.getUrl(ClientWebUrl.HOME);
    }

    /**
     * @param page
     * @param view
     * @param reservationRequest
     * @return view for given attributes
     */
    public WizardView getView(Page page, String view, ReservationRequestModel reservationRequest)
    {
        WizardView wizardView = getWizardView(page, view);
        wizardView.addObject(RESERVATION_REQUEST_ATTRIBUTE, reservationRequest);
        wizardView.addAction(ClientWebUrl.format(ClientWebUrl.WIZARD_MODIFY_CANCEL, reservationRequest.getId()),
                "views.button.cancel", WizardView.ActionPosition.LEFT);
        wizardView.addAction(WizardController.SUBMIT_RESERVATION_REQUEST_FINISH,
                "views.button.finish", WizardView.ActionPosition.RIGHT).setPrimary(true);
        return wizardView;
    }

    /**
     * Handle missing session attributes.
     */
    @ExceptionHandler({HttpSessionRequiredException.class, IllegalStateException.class})
    public Object handleExceptions(Exception exception)
    {
        logger.warn("Redirecting to " + ClientWebUrl.HOME + ".", exception);
        return "redirect:" + ClientWebUrl.HOME;
    }
}
