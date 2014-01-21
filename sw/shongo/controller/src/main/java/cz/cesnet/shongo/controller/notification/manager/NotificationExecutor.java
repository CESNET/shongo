package cz.cesnet.shongo.controller.notification.manager;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.notification.AbstractNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represent an abstract executor of {@link cz.cesnet.shongo.controller.notification.NotificationRecord}s.
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
    public void init(ControllerConfiguration configuration)
    {
    }

    /**
     * @param notification to be executed
     */
    public abstract boolean executeNotification(PersonInformation recipient, AbstractNotification notification);
}
