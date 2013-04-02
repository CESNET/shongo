package cz.cesnet.shongo.controller.notification;

/**
 * {@link Notification} for simple message.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class MessageNotification extends Notification
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
    public MessageNotification(String name, String message)
    {
        this.name = name;
        this.message = message;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getContent()
    {
        return message;
    }
}
