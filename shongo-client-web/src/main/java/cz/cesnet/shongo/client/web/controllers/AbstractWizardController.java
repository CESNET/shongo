package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.client.web.support.MessageProviderImpl;
import cz.cesnet.shongo.client.web.support.Page;
import cz.cesnet.shongo.client.web.WizardPage;
import org.springframework.context.MessageSource;
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
    public static final String RESERVATION_REQUEST_ATTRIBUTE = "reservationRequest";

    @Resource
    protected HttpServletRequest request;

    @Resource
    protected MessageSource messageSource;

    /**
     * Initialize given {@code wizardView} for given {@code currentWizardPageId}.
     *
     * @param wizardView
     * @param currentWizardPageId
     * @param messageProvider
     */
    protected void initWizardPages(WizardView wizardView, Object currentWizardPageId, MessageProvider messageProvider)
    {
    }

    /**
     * @param wizardPageId identifier of the page which should be set as current
     * @param wizardContent name of view which should be used as content for the wizard page
     * @return new {@link WizardView} object initialized by calling the {@link #initWizardPages} method
     */
    protected WizardView getWizardView(Object wizardPageId, String wizardContent)
    {
        MessageProvider messageProvider = MessageProviderImpl.fromRequest(messageSource, request);
        WizardView wizardView = new WizardView();
        initWizardPages(wizardView, wizardPageId, messageProvider);
        wizardView.init(wizardPageId, wizardContent, request.getRequestURI());
        return wizardView;
    }

    /**
     * Represents a {@link ModelAndView} for a wizard.
     */
    public static class WizardView extends ModelAndView
    {
        /**
         * Linked map of {@link WizardPage}s in the top of the view.
         */
        private LinkedHashMap<Object, WizardPage> pages = new LinkedHashMap<Object, WizardPage>();

        /**
         * List of wizard actions at the bottom of the view.
         */
        private final List<Action> actions = new LinkedList<Action>();

        /**
         * Previous action.
         */
        private Action actionPrevious;

        /**
         * Next action.
         */
        private Action actionNext;

        /**
         * Constructor.
         */
        public WizardView()
        {
            super("wizard");
            addObject("wizardPages", pages.values());
            addObject("wizardActions", actions);
        }

        /**
         * Initialize wizard - the current page, the page content and all wizard page by request URI.
         *
         * @param wizardPageId
         * @param wizardContent
         * @param requestUri
         */
        protected void init(Object wizardPageId, String wizardContent, String requestUri)
        {
            // Find current, previous and next page
            WizardPage wizardPageActive = null;
            WizardPage wizardPagePrevious = null;
            WizardPage wizardPageNext = null;
            for (WizardPage wizardPage : pages.values()) {
                if (wizardPageId == null) {
                    // Last page should be current page
                    wizardPagePrevious = wizardPageActive;
                    wizardPageActive = wizardPage;
                }
                else if (wizardPageId == wizardPage.getId()) {
                    wizardPageActive = wizardPage;
                }
                else {
                    if (wizardPageActive == null) {
                        wizardPagePrevious = wizardPage;
                    }
                    else {
                        wizardPageNext = wizardPage;
                        break;
                    }
                }
            }
            addObject("wizardContent", wizardContent);
            addObject("wizardPageActive", wizardPageActive);

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

            // Set all pages until current page as available
            boolean available = wizardPageActive != null;
            for (WizardPage wizardPage : pages.values()) {
                wizardPage.setAvailable(available);
                if (wizardPageActive == wizardPage) {
                    available = false;
                }
            }

            // Update page urls
            if (wizardPageActive != null) {
                Map<String, String> attributes = wizardPageActive.parseUrlAttributes(requestUri, false);
                for (WizardPage wizardPage : pages.values()) {
                    if (wizardPage.isAvailable()) {
                        String wizardPageUrl = wizardPage.getUrl(attributes);
                        wizardPage.setUrl(wizardPageUrl);
                    }
                }
            }
        }

        /**
         * @param wizardPage to be added to the {@link #pages}
         */
        public void addPage(WizardPage wizardPage)
        {
            pages.put(wizardPage.getId(), wizardPage);
        }

        /**
         * @return active wizard page
         */
        public WizardPage getCurrentPage()
        {
            return (WizardPage) getModel().get("wizardPageActive");
        }

        /**
         * @param url
         * @param titleCode
         * @return new {@link Action} constructed from given parameters and added to the {@link #actions}
         */
        public Action addAction(String url, String titleCode)
        {
            return addAction(url, titleCode, ActionPosition.LEFT);
        }

        /**
         * @param url
         * @param titleCode
         * @param position
         * @return new {@link Action} constructed from given parameters and added to the {@link #actions}
         */
        public Action addAction(String url, String titleCode, ActionPosition position)
        {
            Action action = new Action(url, titleCode, position);
            actions.add(action);
            return action;
        }

        /**
         * @param url
         * @param titleCode
         * @return new {@link Action} constructed from given parameters and added to the {@link #actions}
         * at specified {@code index}
         */
        public Action addAction(int index, String url, String titleCode)
        {
            return addAction(index, url, titleCode, ActionPosition.LEFT);
        }

        /**
         * @param url
         * @param titleCode
         * @param position
         * @return new {@link Action} constructed from given parameters and added to the {@link #actions}
         * at specified {@code index}
         */
        public Action addAction(int index, String url, String titleCode, ActionPosition position)
        {
            Action action = new Action(url, titleCode, position);
            actions.add(index, action);
            return action;
        }

        /**
         * @param url of action to be removed
         */
        public void removeAction(String url)
        {
            Iterator<Action> iterator = actions.iterator();
            while (iterator.hasNext()) {
                Action action = iterator.next();
                if (url.equals(action.getUrl())) {
                    iterator.remove();
                }
            }
        }

        /**
         * @return {@link #actionPrevious}
         */
        public Action getActionPrevious()
        {
            return actionPrevious;
        }

        /**
         * @return {@link #actionNext}
         */
        public Action getActionNext()
        {
            return actionNext;
        }

        /**
         * @param url sets the {@link #actionNext#url}
         */
        public void setNextPageUrl(String url)
        {
            actionNext.setUrl(url);
            updatePrimary();
        }

        /**
         * @param url sets the {@link #actionPrevious#url}
         */
        public void setPreviousPageUrl(String url)
        {
            actionPrevious.setUrl(url);
            updatePrimary();
        }

        /**
         * Update which action (previous/next) should be primary.
         */
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
