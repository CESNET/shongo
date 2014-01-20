package cz.cesnet.shongo.controller.notification.manager;

import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.notification.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represent an abstract executor of {@link cz.cesnet.shongo.controller.notification.event.Notification}s.
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
    public abstract void executeNotification(Recipient recipient, Notification notification);

    /**
     * Recipient
     */
    public static class Recipient
    {
        private final String email;

        public Recipient(String email)
        {
            this.email = email;
        }

        public String getEmail()
        {
            return email;
        }

        @Override
        public String toString()
        {
            return String.format(Recipient.class.getSimpleName() + " (email: %s", email);
        }
    }
}
