package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.controller.Authorization;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.reservation.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents a {@link Component} for executing {@link Notification}s by multiple {@link NotificationExecutor}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NotificationManager extends Component implements Component.AuthorizationAware
{
    private static Logger logger = LoggerFactory.getLogger(NotificationManager.class);

    /**
     * @see cz.cesnet.shongo.controller.Authorization
     */
    private Authorization authorization;

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
     * @return {@link Authorization}
     */
    public Authorization getAuthorization()
    {
        return authorization;
    }

    /**
     * @param notification to be executed
     */
    public void executeNotification(Notification notification)
    {
        if (notification.getRecipients().size() == 0) {
            logger.warn("Notification '{}' doesn't have any recipients.", notification.getName());
            return;
        }
        // Execute notification in all executors
        for (NotificationExecutor notificationExecutor : notificationExecutors) {
            notificationExecutor.executeNotification(notification);
        }
    }

    /**
     * Notify about new reservations.
     *
     * @param newReservations
     * @param modifiedReservations
     * @param deletedReservations
     */
    public void notifyReservations(Set<Reservation> newReservations, Set<Reservation> modifiedReservations,
            Set<Reservation> deletedReservations, EntityManager entityManager)
    {
        if (newReservations.size() == 0 && modifiedReservations.size() == 0 && deletedReservations.size() == 0) {
            return;
        }
        logger.debug("Notifying about changes in reservations...");
        for (Reservation reservation : newReservations) {
            executeNotification(new ReservationNotification(
                    ReservationNotification.Type.NEW, reservation, entityManager));
        }
        for (Reservation reservation : modifiedReservations) {
            executeNotification(new ReservationNotification(
                    ReservationNotification.Type.MODIFIED, reservation, entityManager));
        }
        for (Reservation reservation : deletedReservations) {
            executeNotification(new ReservationNotification(
                    ReservationNotification.Type.DELETED, reservation, entityManager));
        }
    }
}
