package cz.cesnet.shongo.controller.notification.manager;

import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.notification.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represent an abstract executor of {@link cz.cesnet.shongo.controller.notification.Notification}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class NotificationExecutor
{
    protected static Logger logger = LoggerFactory.getLogger(NotificationExecutor.class);

    /**
     * Initialize {@link NotificationExecutor}.
     *
     * @param configuration from which the {@link NotificationExecutor} can load settings
     */
    public void init(Configuration configuration)
    {
    }

    /**
     * @param notification to be executed
     */
    public abstract void executeNotification(Notification notification);
}
