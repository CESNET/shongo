package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.WizardPage;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Controller for displaying wizard interface.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractWizardController
{
    protected abstract void initWizardPages(List<WizardPage> wizardPages);

    protected WizardView getWizardView(Object wizardPageId, String wizardContent)
    {
        List<WizardPage> wizardPages = new LinkedList<WizardPage>();
        initWizardPages(wizardPages);

        // Find current, previous and next page
        WizardPage wizardPageCurrent = null;
        WizardPage wizardPagePrevious = null;
        WizardPage wizardPageNext = null;
        for (WizardPage wizardPage : wizardPages) {
            if (wizardPageId == wizardPage.getId()) {
                wizardPageCurrent = wizardPage;
            }
            else {
                if (wizardPageCurrent == null) {
                    wizardPagePrevious = wizardPage;
                }
                else {
                    wizardPageNext = wizardPage;
                    break;
                }
            }
        }

        // Set all pages until current page as available
        boolean available = wizardPageCurrent != null;
        for (WizardPage wizardPage : wizardPages) {
            wizardPage.setAvailable(available);
            if (wizardPageCurrent == wizardPage) {
                available = false;
            }
        }

        WizardView wizardView = new WizardView();
        wizardView.addObject("wizardContent", wizardContent);
        wizardView.addObject("wizardPages", wizardPages);
        wizardView.addObject("wizardPageCurrent", wizardPageCurrent);
        wizardView.addObject("wizardPagePrevious", wizardPagePrevious);
        wizardView.addObject("wizardPageNext", wizardPageNext);
        return wizardView;
    }

    protected static class WizardView extends ModelAndView
    {
        public WizardView()
        {
            super("wizard");
        }

        public void setNextPage(String nextPageUrl)
        {
            if (nextPageUrl == null) {
                nextPageUrl = "";
            }
            addObject("wizardPageNextUrl", nextPageUrl);
        }

        public void setNextPage(String nextPageUrl, String nextPageTitle)
        {
            setNextPage(nextPageUrl);
            addObject("wizardPageNextTitle", nextPageTitle);
        }
    }
}
