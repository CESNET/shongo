package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.SimplePersistentObject;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a notification (e.g., email notification).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Notification extends SimplePersistentObject
{
    /**
     * Date/time when the {@link Notification} was created.
     */
    private DateTime createdAt;

    /**
     * Recipient user-id (if available).
     */
    private String userId;

    /**
     * Recipient email address (if available).
     */
    private String recipientEmail;

    /**
     * Reply-to user-ids.
     */
    private Set<String> replyToUserIds = new HashSet<String>();

    /**
     * Languages which are used in the notification.
     */
    private Set<String> languages = new HashSet<String>();

    /**
     * Notification title (e.g., it can be used as email subject).
     */
    private String title;

    /**
     * Notification content (e.g., it can be used as email content).
     */
    private String message;

    /**
     * @return {@link #createdAt}
     */
    @Column
    @Type(type = "DateTime")
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
     * @return {@link #userId}
     */
    @Column
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    /**
     * @return {@link #recipientEmail}
     */
    @Column
    public String getRecipientEmail()
    {
        return recipientEmail;
    }

    /**
     * @param recipientEmail sets the {@link #recipientEmail}
     */
    public void setRecipientEmail(String recipientEmail)
    {
        this.recipientEmail = recipientEmail;
    }

    /**
     * @return {@link #replyToUserIds}
     */
    @ElementCollection
    @Access(AccessType.FIELD)
    public Set<String> getReplyToUserIds()
    {
        return replyToUserIds;
    }

    /**
     * @param replyToUserIds sets the {@link #replyToUserIds}
     */
    public void setReplyToUserIds(Set<String> replyToUserIds)
    {
        this.replyToUserIds.clear();
        this.replyToUserIds.addAll(replyToUserIds);
    }

    /**
     * @param replyToUserId to be added to the {@link #replyToUserIds}
     */
    public void addReplyToUserId(String replyToUserId)
    {
        replyToUserIds.add(replyToUserId);
    }

    /**
     * @return {@link #languages}
     */
    @ElementCollection
    @Access(AccessType.FIELD)
    public Set<String> getLanguages()
    {
        return languages;
    }

    /**
     * @param languages sets the {@link #languages}
     */
    public void setLanguages(Set<String> languages)
    {
        this.languages.clear();
        this.languages.addAll(languages);
    }

    /**
     * @param language to be added to the {@link #languages}
     */
    public void addLanguage(String language)
    {
        languages.add(language);
    }

    /**
     * @return {@link #title}
     */
    @Column(nullable = false)
    public String getTitle()
    {
        return title;
    }

    /**
     * @param title sets the {@link #title}
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * @return {@link #message}
     */
    @Column(nullable = false)
    public String getMessage()
    {
        return message;
    }

    /**
     * @param message sets the {@link #message}
     */
    public void setMessage(String message)
    {
        this.message = message;
    }

    @Override
    public String toString()
    {
        return String.format(Notification.class.getSimpleName() + " (id: %d, title: %s)", id, title);
    }

    /**
     * Available states of {@link Notification}s.
     */
    public static enum State
    {
        /**
         * {@link Notification} hasn't been performed yet.
         */
        PREPARED,

        /**
         * {@link Notification} has been performed.
         */
        PERFORMED,

        /**
         * Performing of the {@link Notification} has failed.
         */
        FAILED
    }
}