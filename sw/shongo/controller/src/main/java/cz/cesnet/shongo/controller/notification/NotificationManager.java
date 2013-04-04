package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.authorization.Authorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a {@link Component} for executing {@link Notification}s by multiple {@link NotificationExecutor}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NotificationManager extends Component implements Component.AuthorizationAware
{
    private static Logger logger = LoggerFactory.getLogger(NotificationManager.class);

    /**
     * @see cz.cesnet.shongo.controller.authorization.Authorization
     */
    private Authorization authorization;

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
    public void setAuthorization(Authorization authorization)
    {
        this.authorization = authorization;
    }

    /**
     * @return {@link Authorization}
     */
    public Authorization getAuthorization()
    {
        return authorization;
    }

    @Override
    public void init(Configuration configuration)
    {
        checkDependency(authorization, Authorization.class);
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
    public void executeNotification(Notification notification)
    {
        if (!enabled) {
            logger.warn("Notification '{}' cannot be executed because notifications are disabled.",
                    notification.getName());
            return;
        }
        if (redirectTo != null) {
            logger.warn("Notification '{}' is redirected to (name: {}, organization: {}, email: {}).", new Object[]{
                    notification.getName(),
                    redirectTo.getFullName(), redirectTo.getRootOrganization(), redirectTo.getPrimaryEmail()
            });
            notification.clearRecipients();
            notification.addRecipient(redirectTo);
        }
        if (notification.getRecipients().size() == 0) {
            logger.warn("Notification '{}' doesn't have any recipients.", notification.getName());
            return;
        }
        // Execute notification in all executors
        for (NotificationExecutor notificationExecutor : notificationExecutors) {
            notificationExecutor.executeNotification(notification);
        }
    }
}
