package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.util.TemporalHelper;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Represent an abstract executor of {@link Notification}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class NotificationExecutor
{
    protected static Logger logger = LoggerFactory.getLogger(NotificationExecutor.class);

    /**
     * @param notification to be executed
     */
    public abstract void executeNotification(Notification notification);

    /**
     * Initialize {@link NotificationExecutor}.
     *
     * @param configuration from which the {@link NotificationExecutor} can load settings
     */
    public void init(Configuration configuration)
    {
    }
}
