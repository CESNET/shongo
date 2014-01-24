package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.notification.executor.NotificationExecutor;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Represents a {@link Component} for executing {@link NotificationRecord}s by multiple {@link NotificationExecutor}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NotificationManager extends Component implements Component.AuthorizationAware
{
    private static Logger logger = LoggerFactory.getLogger(NotificationManager.class);

    /**
     * @see Authorization
     */
    private Authorization authorization;

    /**
     * List of {@link NotificationExecutor}s for executing {@link NotificationRecord}s.
     */
    private List<NotificationExecutor> notificationExecutors = new ArrayList<NotificationExecutor>();

    /**
     * Specifies whether the manager should execute notifications or skip them.
     */
    private boolean enabled = true;

    /**
     * {@link PersonInformation} to which all {@link NotificationRecord}s should be redirected.
     */
    private PersonInformation redirectTo = null;

    /**
     * List of {@link AbstractNotification}s to be executed.
     */
    private List<AbstractNotification> notifications = new LinkedList<AbstractNotification>();

    /**
     * Map of {@link ReservationRequestNotification} by {@link AbstractReservationRequest#id}.
     */
    protected Map<Long, ReservationRequestNotification> reservationRequestNotificationsById =
            new HashMap<Long, ReservationRequestNotification>();

    /**
     * Map of {@link ReservationRequestNotification} by {@link AbstractReservationRequest#id}.
     */
    protected Map<Long, List<RoomNotification>> roomNotificationsByReservationRequestId =
            new HashMap<Long, List<RoomNotification>>();

    /**
     * @return {@link #authorization}
     */
    public Authorization getAuthorization()
    {
        return authorization;
    }

    @Override
    public void setAuthorization(Authorization authorization)
    {
        this.authorization = authorization;
    }

    /**
     * @param notificationExecutor to be added to the {@link #notificationExecutors}
     */
    public synchronized void addNotificationExecutor(NotificationExecutor notificationExecutor)
    {
        notificationExecutors.add(notificationExecutor);
    }

    /**
     * @return true whether {@link #notifications} is not empty,
     *         false otherwise
     */
    public boolean hasNotifications()
    {
        return !notifications.isEmpty();
    }

    @Override
    public synchronized void init(ControllerConfiguration configuration)
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
    public synchronized void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * @param redirectTo sets the {@link #redirectTo}
     */
    public synchronized void setRedirectTo(PersonInformation redirectTo)
    {
        this.redirectTo = redirectTo;
    }

    /**
     * @param notification to be added to the {@link #notifications}
     */
    public synchronized void addNotification(AbstractNotification notification, EntityManager entityManager)
    {
        notifications.add(notification);

        notification.onAdded(this, entityManager);
    }

    /**
     * @param notifications to be added to the {@link #notifications}
     */
    public synchronized void addNotifications(List<AbstractNotification> notifications, EntityManager entityManager)
    {
        for (AbstractNotification notification : notifications) {
            addNotification(notification, entityManager);
        }
    }

    /**
     * @param reservationRequestId
     * @return list of {@link RoomNotification} for given {@code reservationRequestId}
     */
    protected synchronized List<RoomNotification> getRoomNotificationsByReservationRequestId(Long reservationRequestId)
    {
        List<RoomNotification> roomNotifications = roomNotificationsByReservationRequestId.get(reservationRequestId);
        if (roomNotifications == null) {
            roomNotifications = new LinkedList<RoomNotification>();
            roomNotificationsByReservationRequestId.put(reservationRequestId, roomNotifications);
        }
        return roomNotifications;
    }

    /**
     * @param dateTime
     * @param entityManager to be used
     */
    public synchronized void executeNotifications(DateTime dateTime, EntityManager entityManager)
    {
        entityManager.getTransaction().begin();
        for (Iterator<AbstractNotification> iterator = notifications.iterator(); iterator.hasNext(); ) {
            AbstractNotification notification = iterator.next();
            if (dateTime != null && !notification.isReady(dateTime)) {
                continue;
            }
            executeNotification(notification, entityManager);
            notification.onRemoved(this, entityManager);
            iterator.remove();
        }
        entityManager.getTransaction().commit();
    }

    /**
     * @param notification to be executed
     */
    private void executeNotification(AbstractNotification notification, EntityManager entityManager)
    {
        // Get recipients (or redirect)
        Collection<PersonInformation> recipients = notification.getRecipients();
        if (redirectTo != null) {
            logger.warn("Notification '{}' is redirected to (name: {}, organization: {}, email: {}).", new Object[]{
                    notification,
                    redirectTo.getFullName(), redirectTo.getRootOrganization(), redirectTo.getPrimaryEmail()
            });
            recipients = new LinkedList<PersonInformation>();
            recipients.add(redirectTo);
        }

        if (!enabled) {
            logger.warn("Notification '{}' cannot be executed because notifications are disabled.", notification);
        }

        for (PersonInformation recipient : recipients) {
            boolean result = false;
            if (enabled) {
                // Perform notification in every notification executor
                for (NotificationExecutor notificationExecutor : notificationExecutors) {
                    if (notificationExecutor.executeNotification(recipient, notification, this)) {
                        result = true;
                    }
                }
            }
            else {
                // Notifications are disabled, act it has been performed successfully
                result = true;
            }
            if (result && entityManager != null) {
                // Create persistent notification record
                NotificationRecord notificationRecord = notification.createRecord(recipient, entityManager);
                if (notificationRecord != null) {
                    entityManager.persist(notificationRecord);
                }
            }
        }
    }
}
