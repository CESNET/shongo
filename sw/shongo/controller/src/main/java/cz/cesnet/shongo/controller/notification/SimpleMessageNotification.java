package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;

import java.util.Locale;

/**
 * {@link Notification} for simple message.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SimpleMessageNotification extends AbstractNotification
{
    /**
     * Message title.
     */
    private String name;

    /**
     * Message about which the recipients should be notified.
     */
    private String message;

    /**
     * Constructor.
     *
     * @param name    sets the {@link #name}
     * @param message sets the {@link #message}
     */
    public SimpleMessageNotification(String name, String message)
    {
        this.name = name;
        this.message = message;
    }

    @Override
    protected NotificationMessage renderMessageForRecipient(PersonInformation recipient)
    {
        return new NotificationMessage(Locale.getDefault().getLanguage(), name, message);
    }
}
