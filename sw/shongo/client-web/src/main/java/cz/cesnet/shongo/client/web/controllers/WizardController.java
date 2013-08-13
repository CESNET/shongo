package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.WizardPage;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for displaying wizard interface.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Controller
public class WizardController extends AbstractWizardController
{
    public static final String SUBMIT_RESERVATION_REQUEST = "javascript: " +
            "document.getElementById('reservationRequest').submit();";

    public static final String SUBMIT_RESERVATION_REQUEST_FINISH = "javascript: " +
            "$('form#reservationRequest').append('<input type=\\'hidden\\' name=\\'finish\\' value=\\'true\\'/>');" +
            "document.getElementById('reservationRequest').submit();";

    private static enum Page
    {
        SELECT
    }

    public static WizardPage createSelectWizardPage()
    {
        return new WizardPage(Page.SELECT, ClientWebUrl.WIZARD_SELECT, "views.wizard.page.select");
    }

    @Override
    protected void initWizardPages(WizardView wizardView, Object currentWizardPageId)
    {
        wizardView.addPage(createSelectWizardPage());
        wizardView.addPage(new WizardPage(null, null, "views.wizard.page.multiple"));
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
    public ModelAndView handleSelect(SessionStatus sessionStatus, Model model)
    {
        return getWizardView(Page.SELECT, "wizardSelect.jsp");
    }
}
