package cz.cesnet.shongo.controller.notification.manager;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.notification.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a {@link Component} for executing {@link Notification}s by multiple {@link NotificationExecutor}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NotificationManager extends Component
{
    private static Logger logger = LoggerFactory.getLogger(NotificationManager.class);

    /**
     * List of {@link NotificationExecutor}s for executing {@link Notification}s.
     */
    private List<NotificationExecutor> notificationExecutors = new ArrayList<NotificationExecutor>();

    /**
     * Specifies whether the manager should execute notifications or skip them.
     */
    private boolean enabled = true;

    /**
     * {@link PersonInformation} to which all {@link Notification}s should be redirected.
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
     * @param notification
     * @return {@link NotificationExecutor.Recipient} for given {@code notification}
     */
    public NotificationExecutor.Recipient getRecipient(Notification notification)
    {
        if (redirectTo != null) {
            logger.warn("Notification '{}' is redirected to (name: {}, organization: {}, email: {}).", new Object[]{
                    notification,
                    redirectTo.getFullName(), redirectTo.getRootOrganization(), redirectTo.getPrimaryEmail()
            });
            return new NotificationExecutor.Recipient(redirectTo.getPrimaryEmail());
        }
        String recipientEmail = notification.getRecipientEmail();
        if (recipientEmail == null) {
            throw new IllegalStateException(notification + " doesn't have email recipient.");
        }
        return new NotificationExecutor.Recipient(recipientEmail);
    }

    /**
     * Execute not-executed notifications.
     *
     * @param entityManager to be used
     */
    public void executeNotifications(EntityManager entityManager)
    {
        if (!hasExecutors()) {
            return;
        }

        logger.debug("Executing notifications...");
        throw new TodoImplementException("load and perform notifications");
    }

    /**
     * @param notification to be executed
     */
    public void executeNotification(Notification notification)
    {
        if (!enabled) {
            logger.warn("Notification '{}' cannot be executed because notifications are disabled.", notification);
            return;
        }

        // Execute notification in all executors
        NotificationExecutor.Recipient recipient = getRecipient(notification);
        for (NotificationExecutor notificationExecutor : notificationExecutors) {
            notificationExecutor.executeNotification(recipient, notification);
        }
    }
}
