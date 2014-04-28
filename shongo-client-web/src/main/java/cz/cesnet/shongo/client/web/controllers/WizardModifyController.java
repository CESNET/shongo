package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.WizardPage;
import cz.cesnet.shongo.client.web.models.SpecificationType;
import cz.cesnet.shongo.client.web.support.BackUrl;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.client.web.support.editors.*;
import cz.cesnet.shongo.controller.api.SecurityToken;
import org.joda.time.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

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
            HttpServletRequest request,
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        BackUrl.applyTo(request, ClientWebUrl.format(ClientWebUrl.WIZARD_MODIFY, reservationRequestId));
        WizardView wizardView = getWizardView(Page.EXTEND, "wizardModifyExtend.jsp");
        wizardView.addAction(ClientWebUrl.format(ClientWebUrl.WIZARD_MODIFY_CANCEL, reservationRequestId),
                "views.button.cancel", WizardView.ActionPosition.LEFT);
        return wizardView;
    }

    /**
     * Enlarge existing virtual room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_MODIFY_ENLARGE, method = RequestMethod.GET)
    public ModelAndView handleEnlarge(
            HttpServletRequest request,
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        BackUrl.applyTo(request, ClientWebUrl.format(ClientWebUrl.WIZARD_MODIFY, reservationRequestId));
        WizardView wizardView = getWizardView(Page.ENLARGE, "wizardModifyEnlarge.jsp");
        wizardView.addAction(ClientWebUrl.format(ClientWebUrl.WIZARD_MODIFY_CANCEL, reservationRequestId),
                "views.button.cancel", WizardView.ActionPosition.LEFT);
        return wizardView;
    }

    /**
     * Enlarge existing virtual room.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_MODIFY_RECORDED, method = RequestMethod.GET)
    public ModelAndView handleRecorded(
            HttpServletRequest request,
            SecurityToken securityToken,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        BackUrl.applyTo(request, ClientWebUrl.format(ClientWebUrl.WIZARD_MODIFY, reservationRequestId));
        WizardView wizardView = getWizardView(Page.RECORDED, "wizardModifyRecorded.jsp");
        wizardView.addAction(ClientWebUrl.format(ClientWebUrl.WIZARD_MODIFY_CANCEL, reservationRequestId),
                "views.button.cancel", WizardView.ActionPosition.LEFT);
        return wizardView;
    }

    /**
     * Cancel the modification.
     */
    @RequestMapping(value = ClientWebUrl.WIZARD_MODIFY_CANCEL, method = RequestMethod.GET)
    public Object handleCancel(
            SessionStatus sessionStatus,
            @PathVariable(value = "reservationRequestId") String reservationRequestId)
    {
        sessionStatus.setComplete();
        BackUrl backUrl = BackUrl.getInstance(request,
                ClientWebUrl.format(ClientWebUrl.WIZARD_MODIFY, reservationRequestId));
        return "redirect:" + backUrl.getUrl(ClientWebUrl.HOME);
    }
}
