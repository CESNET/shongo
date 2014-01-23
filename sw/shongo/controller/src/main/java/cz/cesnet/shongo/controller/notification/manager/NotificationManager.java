package cz.cesnet.shongo.controller.notification.manager;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.notification.NotificationRecord;
import cz.cesnet.shongo.controller.notification.AbstractNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Represents a {@link Component} for executing {@link cz.cesnet.shongo.controller.notification.NotificationRecord}s by multiple {@link NotificationExecutor}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NotificationManager extends Component
{
    private static Logger logger = LoggerFactory.getLogger(NotificationManager.class);

    /**
     * List of {@link NotificationExecutor}s for executing {@link cz.cesnet.shongo.controller.notification.NotificationRecord}s.
     */
    private List<NotificationExecutor> notificationExecutors = new ArrayList<NotificationExecutor>();

    /**
     * Specifies whether the manager should execute notifications or skip them.
     */
    private boolean enabled = true;

    /**
     * {@link PersonInformation} to which all {@link cz.cesnet.shongo.controller.notification.NotificationRecord}s should be redirected.
     */
    private PersonInformation redirectTo = null;

    /**
     * @param notificationExecutor to be added to the {@link #notificationExecutors}
     */
    public void addNotificationExecutor(NotificationExecutor notificationExecutor)
    {
        notificationExecutors.add(notificationExecutor);
    }

    /**
     * @return true if at least one {@link NotificationExecutor} is available,
     *         false otherwise
     */
    public boolean hasExecutors()
    {
        return notificationExecutors.size() > 0;
    }

    @Override
    public void init(ControllerConfiguration configuration)
    {
        super.init(configuration);

        // Initialize all executors
        for (NotificationExecutor notificationExecutor : notificationExecutors) {
            notificationExecutor.init(configuration);
        }
    }

    /**
     * @param enabled sets the {@link #enabled}
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * @param redirectTo sets the {@link #redirectTo}
     */
    public void setRedirectTo(PersonInformation redirectTo)
    {
        this.redirectTo = redirectTo;
    }

    /**
     * @param notification to be executed
     */
    public void executeNotification(AbstractNotification notification, EntityManager entityManager)
    {
        // Get recipients (or redirect)
        Collection<PersonInformation> recipients = notification.getRecipients();
        if (redirectTo != null) {
            logger.warn("Notification '{}' is redirected to (name: {}, organization: {}, email: {}).", new Object[]{
                    notification,
                    redirectTo.getFullName(), redirectTo.getRootOrganization(), redirectTo.getPrimaryEmail()
            });
            recipients = new LinkedList<PersonInformation>();
            recipients.add(redirectTo);
        }

        if (!enabled) {
            logger.warn("Notification '{}' cannot be executed because notifications are disabled.", notification);
        }

        for (PersonInformation recipient : recipients) {
            boolean result = false;
            if (enabled) {
                // Perform notification in every notification executor
                for (NotificationExecutor notificationExecutor : notificationExecutors) {
                    if (notificationExecutor.executeNotification(recipient, notification, this)) {
                        result = true;
                    }
                }
            }
            else {
                // Notifications are disabled, act it has been performed successfully
                result = true;
            }
            if (result && entityManager != null) {
                // Create persistent notification record
                NotificationRecord notificationRecord = notification.createRecord(recipient, entityManager);
                if (notificationRecord != null) {
                    entityManager.persist(notificationRecord);
                }
            }
        }
    }

    /**
     * @param notifications to be executed
     */
    public void executeNotifications(List<AbstractNotification> notifications, EntityManager entityManager)
    {
        for (AbstractNotification notification : notifications) {
            executeNotification(notification, entityManager);
        }
    }
}
