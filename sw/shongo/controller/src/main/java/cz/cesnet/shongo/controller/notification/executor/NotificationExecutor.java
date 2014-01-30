package cz.cesnet.shongo.controller.notification.executor;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.notification.AbstractNotification;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

/**
 * Represent an abstract executor of {@link AbstractNotification}s.
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
     * @param recipient
     * @param notification to be executed
     * @param manager
     * @param entityManager
     */
    public abstract void executeNotification(PersonInformation recipient, AbstractNotification notification,
            NotificationManager manager, EntityManager entityManager);
}
