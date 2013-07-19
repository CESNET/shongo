package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.Page;
import cz.cesnet.shongo.client.web.WizardPage;
import org.springframework.web.servlet.ModelAndView;

import java.util.LinkedList;
import java.util.List;

/**
 * Controller for displaying wizard interface.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractWizardController
{
    protected abstract void initWizardPages(List<WizardPage> wizardPages, Object currentWizardPageId);

    protected WizardView getWizardView(Object wizardPageId, String wizardContent)
    {
        List<WizardPage> wizardPages = new LinkedList<WizardPage>();
        initWizardPages(wizardPages, wizardPageId);

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

        WizardView wizardView = new WizardView(wizardPagePrevious, wizardPageNext);
        wizardView.addObject("wizardContent", wizardContent);
        wizardView.addObject("wizardPages", wizardPages);
        wizardView.addObject("wizardPageCurrent", wizardPageCurrent);
        return wizardView;
    }

    public static class WizardView extends ModelAndView
    {
        private final List<Action> actions = new LinkedList<Action>();

        private final Action actionPrevious;

        private final Action actionNext;

        public WizardView(WizardPage wizardPagePrevious, WizardPage wizardPageNext)
        {
            super("wizard");
            addObject("wizardActions", actions);

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
             * @param url sets the {@link #url}
             */
            protected void setUrl(String url)
            {
                this.url = url;
            }

            /**
             * @param titleCode sets the {@link #titleCode}
             */
            protected void setTitleCode(String titleCode)
            {
                this.titleCode = titleCode;
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
