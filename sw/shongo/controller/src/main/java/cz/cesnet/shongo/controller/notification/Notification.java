package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;

import java.util.Collection;

/**
 * Represents a notification for some recipients.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface Notification
{
    /**
     * @param recipient who should be notified by the {@link Notification}
     * @return true whether given {@code recipient} has been added,
     *         false whether given {@code recipient} already exists
     */
    public boolean addRecipient(PersonInformation recipient);

    /**
     * Remove all added recipients.
     */
    public void clearRecipients();

    /**
     * @return collection of recipients who should be notified by this {@link Notification}
     */
    public Collection<PersonInformation> getRecipients();

    /**
     * @return true whether {@link Notification} has at least one recipient,
     *         false otherwise
     */
    public boolean hasRecipients();

    /**
     * @param recipient for who the {@link NotificationMessage} should be returned
     * @return {@link NotificationMessage} for given {@code recipient}
     */
    public NotificationMessage getRecipientMessage(PersonInformation recipient);

    /**
     * @return collection of reply-to who should be contacted when replying to this {@link Notification}
     */
    public Collection<PersonInformation> getReplyTo();
}
