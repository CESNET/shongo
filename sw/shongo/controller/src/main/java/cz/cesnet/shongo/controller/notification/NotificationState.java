package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.SimplePersistentObject;

import javax.persistence.*;

/**
 * Represents a state of notification by a persistent {@link #sequence} number.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class NotificationState extends SimplePersistentObject
{
    /**
     * Current sequence number.
     */
    @Column
    @Access(AccessType.FIELD)
    private int sequence = 0;

    /**
     * @param notificationState
     * @param entityManager
     * @return {@link NotificationState#sequence} for given {@code notificationState}
     */
    public static synchronized int getSequence(NotificationState notificationState, EntityManager entityManager)
    {
        notificationState = entityManager.find(NotificationState.class, notificationState.getId());
        int sequence = notificationState.sequence;
        notificationState.sequence++;
        return sequence;
    }
}
