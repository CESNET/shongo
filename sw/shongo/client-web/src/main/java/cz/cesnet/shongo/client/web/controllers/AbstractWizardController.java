package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.Page;
import cz.cesnet.shongo.client.web.WizardPage;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Controller for displaying wizard interface.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractWizardController
{
    @Resource
    protected HttpServletRequest request;

    protected void initWizardPages(WizardView wizardView, Object currentWizardPageId)
    {
    }

    protected WizardView getWizardView(Object wizardPageId, String wizardContent)
    {
        WizardView wizardView = new WizardView();
        initWizardPages(wizardView, wizardPageId);
        wizardView.init(wizardPageId, wizardContent, request.getRequestURI());
        return wizardView;
    }

    public static class WizardView extends ModelAndView
    {
        private LinkedHashMap<Object, WizardPage> pages = new LinkedHashMap<Object, WizardPage>();

        private final List<Action> actions = new LinkedList<Action>();

        private Action actionPrevious;

        private Action actionNext;

        public WizardView()
        {
            super("wizard");
            addObject("wizardPages", pages.values());
            addObject("wizardActions", actions);
        }

        protected void init(Object wizardPageId, String wizardContent, String requestUri)
        {
            // Find current, previous and next page
            WizardPage wizardPageCurrent = null;
            WizardPage wizardPagePrevious = null;
            WizardPage wizardPageNext = null;
            for (WizardPage wizardPage : getPages()) {
                if (wizardPageId == null) {
                    // Last page should be current page
                    wizardPagePrevious = wizardPageCurrent;
                    wizardPageCurrent = wizardPage;
                }
                else if (wizardPageId == wizardPage.getId()) {
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
            initNavigationPages(wizardPagePrevious, wizardPageNext);
            addObject("wizardContent", wizardContent);
            addObject("wizardPageCurrent", wizardPageCurrent);

            // Set all pages until current page as available
            boolean available = wizardPageCurrent != null;
            for (WizardPage wizardPage : getPages()) {
                wizardPage.setAvailable(available);
                if (wizardPageCurrent == wizardPage) {
                    available = false;
                }
            }

            // Update page urls
            if (wizardPageCurrent != null) {
                Map<String, String> attributes = wizardPageCurrent.parseUrlAttributes(requestUri, false);
                for (WizardPage wizardPage : getPages()) {
                    if (wizardPage.isAvailable()) {
                        String wizardPageUrl = wizardPage.getUrl(attributes);
                        wizardPage.setUrl(wizardPageUrl);
                    }
                }
            }
        }

        private void initNavigationPages(WizardPage wizardPagePrevious, WizardPage wizardPageNext)
        {
            // Add previous action
            if (wizardPagePrevious != null) {
                actionPrevious = new Action(wizardPagePrevious.getUrl(), "views.button.back", ActionPosition.LEFT);
            }
            else {
                actionPrevious = new Action(null, "views.button.back", ActionPosition.LEFT);
            }
            actions.add(actionPrevious);

            // Add next action
            if (wizardPageNext != null) {
                actionNext = new Action(wizardPageNext.getUrl(), "views.button.continue", ActionPosition.RIGHT);
            }
            else {
                actionNext = new Action(null, "views.button.finish", ActionPosition.RIGHT);
            }
            actions.add(actionNext);

            // Update primary button
            updatePrimary();
        }

        public Collection<WizardPage> getPages()
        {
            return pages.values();
        }

        public void addPage(WizardPage wizardPage)
        {
            pages.put(wizardPage.getId(), wizardPage);
        }

        public void setPageId(WizardPage wizardPage, Object newPageId)
        {
            pages.remove(wizardPage.getId());
            pages.put(newPageId, new WizardPage(newPageId, wizardPage.getUrl(), wizardPage.getTitleCode()));
        }

        public WizardPage getCurrentPage()
        {
            return (WizardPage) getModel().get("wizardPageCurrent");
        }

        public Action addAction(String url, String titleCode)
        {
            return addAction(url, titleCode, ActionPosition.LEFT);
        }

        public Action addAction(String url, String titleCode, ActionPosition position)
        {
            Action action = new Action(url, titleCode, position);
            actions.add(action);
            return action;
        }

        public String getPreviousPageUrl()
        {
            return actionPrevious.getUrl();
        }

        public void setPreviousPage(String url)
        {
            actionPrevious.setUrl(url);
            updatePrimary();
        }

        public void setPreviousPage(String url, String title)
        {
            setPreviousPage(url);
            actionPrevious.setTitleCode(title);
        }

        public void setPreviousPage(String url, String title, boolean primary)
        {
            setPreviousPage(url, title);
            actionPrevious.setPrimary(primary);
        }

        public void setNextPage(String url)
        {
            actionNext.setUrl(url);
            updatePrimary();
        }

        public void setNextPage(String url, String title)
        {
            setNextPage(url);
            actionNext.setTitleCode(title);
        }

        private void updatePrimary()
        {
            boolean nextNotEmpty = actionNext.getUrl() != null;
            actionPrevious.setPrimary(!nextNotEmpty);
            actionNext.setPrimary(nextNotEmpty);
        }

        /**
         * Represents wizard bottom action button.
         */
        public static class Action extends Page
        {
            /**
             * @see ActionPosition
             */
            private ActionPosition position;

            /**
             * Specifies whether action should be highlighted.
             */
            private boolean primary;

            /**
             * Constructor.
             *
             * @param url       sets the {@link #url}
             * @param titleCode sets the {@link #titleCode}
             */
            public Action(String url, String titleCode, ActionPosition position)
            {
                super(url, titleCode);
                this.position = position;
            }

            /**
             * @return {@link #position}
             */
            public ActionPosition getPosition()
            {
                return position;
            }

            /**
             * @return {@link #primary}
             */
            public boolean isPrimary()
            {
                return primary;
            }

            /**
             * @param primary sets the {@link #primary}
             */
            protected void setPrimary(boolean primary)
            {
                this.primary = primary;
            }
        }

        /**
         * {@link Action} position.
         */
        public static enum ActionPosition
        {
            NONE,
            LEFT,
            RIGHT
        }
    }
}
