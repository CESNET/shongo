package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.controller.acl.AclIdentity;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import org.joda.time.DateTime;

import javax.persistence.*;

/**
 * Represents a record that a recipient was notified by a notification about a target.
 *
 * Recipient is identified by {@link #recipientType} and {@link #recipientId}.
 * Type of notification is specified in {@link #notificationType} and it also defines the type of target.
 * The {@link #targetId} identifies the target.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"recipient_type", "recipient_id", "notification_type", "target_id"}))
@NamedQueries({
        @NamedQuery(name="NotificationRecord.find", query="SELECT r FROM NotificationRecord r WHERE r.recipientType = :recipientType AND r.recipientId = :recipientId AND r.notificationType = :notificationType AND r.targetId = :targetId ORDER BY createdAt"),
        @NamedQuery(name="NotificationRecord.findByRecipient", query="SELECT r FROM NotificationRecord r WHERE r.recipientType = :recipientType AND r.recipientId = :recipientId ORDER BY createdAt")
})
public class NotificationRecord extends SimplePersistentObject
{
    /**
     * Date/time when the {@link NotificationRecord} was created.
     */
    private DateTime createdAt;

    /**
     * @see RecipientType
     */
    private RecipientType recipientType;

    /**
     * Recipient identifier.
     */
    private Long recipientId;

    /**
     * @see NotificationType
     */
    private NotificationType notificationType;

    /**
     * Target identifier.
     */
    private Long targetId;

    /**
     * @return {@link #createdAt}
     */
    @Column(nullable = false)
    @org.hibernate.annotations.Type(type = "DateTime")
    @Access(AccessType.FIELD)
    public DateTime getCreatedAt()
    {
        return createdAt;
    }

    /**
     * @param createdAt sets the {@link #createdAt}
     */
    public void setCreatedAt(DateTime createdAt)
    {
        this.createdAt = createdAt;
    }

    /**
     * @return {@link #recipientType}
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public RecipientType getRecipientType()
    {
        return recipientType;
    }

    /**
     * @param recipientType sets the {@link #recipientType}
     */
    public void setRecipientType(RecipientType recipientType)
    {
        this.recipientType = recipientType;
    }

    /**
     * @return {@link #recipientId}
     */
    public Long getRecipientId()
    {
        return recipientId;
    }

    /**
     * @param recipientId sets the {@link #recipientId}
     */
    public void setRecipientId(Long recipientId)
    {
        this.recipientId = recipientId;
    }

    /**
     * @return {@link #notificationType}
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public NotificationType getNotificationType()
    {
        return notificationType;
    }

    /**
     * @param type sets the {@link #notificationType}
     */
    public void setNotificationType(NotificationType type)
    {
        this.notificationType = type;
    }

    /**
     * @return {@link #targetId}
     */
    public Long getTargetId()
    {
        return targetId;
    }

    /**
     * @param targetId sets the {@link #targetId}
     */
    public void setTargetId(Long targetId)
    {
        this.targetId = targetId;
    }

    @Override
    public String toString()
    {
        return String.format(NotificationRecord.class.getSimpleName() + " (created: %s, recipient: %s:%s, type: %s, target: %s)",
                createdAt, recipientType, recipientId, notificationType, targetId);
    }

    /**
     * Types of recipients. It specifies what does {@link #recipientId} means.
     */
    public static enum RecipientType
    {
        /**
         * Recipient is {@link AbstractParticipant} and thus {@link #recipientId} means {@link AbstractParticipant#id}.
         */
        PARTICIPANT,

        /**
         * Recipient is user and thus {@link #recipientId} means {@link AclIdentity#id}.
         */
        USER
    }

    /**
     * Types of notifications which a recipient can receive.
     */
    public static enum NotificationType
    {
        /**
         * {@link NotificationRecord} that a the {@link AbstractParticipant} has been configured to
         * a {@link RoomEndpoint}.
         */
        ROOM_CREATED,

        /**
         * {@link NotificationRecord} that a {@link RoomEndpoint} in which {@link AbstractParticipant}
         * is configured will start soon.
         */
        ROOM_START,

        /**
         * {@link NotificationRecord} that a {@link RoomEndpoint} in which {@link AbstractParticipant}
         * is configured was modified.
         */
        ROOM_MODIFIED,

        /**
         * {@link NotificationRecord} that a {@link RoomEndpoint} in which {@link AbstractParticipant}
         * is configured was deleted.
         */
        ROOM_DELETED,
    }
}