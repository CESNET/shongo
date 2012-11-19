package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a {@link Component} for executing {@link Notification}s by multiple {@link NotificationExecutor}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NotificationManager extends Component
{
    /**
     * List of {@link NotificationExecutor}s for executing {@link Notification}s.
     */
    private List<NotificationExecutor> notificationExecutors = new ArrayList<NotificationExecutor>();

    /**
     * @param notificationExecutor to be added to the {@link #notificationExecutors}
     */
    public void addNotificationExecutor(NotificationExecutor notificationExecutor)
    {
        notificationExecutors.add(notificationExecutor);
    }

    @Override
    public void init(Configuration configuration)
    {
        super.init(configuration);

        // Initialize all executors
        for ( NotificationExecutor notificationExecutor : notificationExecutors) {
            notificationExecutor.init(configuration);
        }
    }

    /**
     * @param notification to be executed
     */
    public void executeNotification(Notification notification)
    {
        // Execute notification in all executors
        for ( NotificationExecutor notificationExecutor : notificationExecutors) {
            notificationExecutor.executeNotification(notification);
        }
    }
}
