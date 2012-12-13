package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.controller.Authorization;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.fault.FaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a {@link Component} for executing {@link Notification}s by multiple {@link NotificationExecutor}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NotificationManager extends Component implements Component.EntityManagerFactoryAware,
                                                              Component.DomainAware, Component.AuthorizationAware
{
    private static Logger logger = LoggerFactory.getLogger(NotificationManager.class);

    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see cz.cesnet.shongo.controller.Domain
     */
    private Domain domain;

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

    @Override
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void setDomain(cz.cesnet.shongo.controller.Domain domain)
    {
        this.domain = domain;
    }

    @Override
    public void setAuthorization(Authorization authorization)
    {
        this.authorization = authorization;
    }

    @Override
    public void init(Configuration configuration)
    {
        checkDependency(entityManagerFactory, EntityManagerFactory.class);
        checkDependency(domain, cz.cesnet.shongo.controller.Domain.class);
        checkDependency(authorization, Authorization.class);
        super.init(configuration);

        // Initialize all executors
        for (NotificationExecutor notificationExecutor : notificationExecutors) {
            notificationExecutor.init(configuration);
        }
    }

    /**
     * @return {@link EntityManager}
     */
    public EntityManager createEntityManager()
    {
        return entityManagerFactory.createEntityManager();
    }

    /**
     * @return {@link #domain}
     */
    public Domain getDomain()
    {
        return domain;
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
     * @param reservations
     */
    public void notifyNewReservations(List<Reservation> reservations)
    {
        if (reservations.size() == 0) {
            return;
        }
        logger.debug("Notifying about new reservations...");
        for (Reservation reservation : reservations) {
            executeNotification(new NewReservationNotification(reservation, this));
        }
    }
}
