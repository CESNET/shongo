package cz.cesnet.shongo.controller.notification;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a notification.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Notification
{
    // TODO: Reference person to who the notification is addressed

    /**
     * Name of the description.
     */
    private String name;

    /**
     * Text of the notification.
     */
    private String text;

    /**
     * List of child {@link Notification}s.
     */
    private List<Notification> childNotifications = new ArrayList<Notification>();

    /**
     * @param name set the {@link #name}
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return {@link #text}
     */
    public final String getText()
    {
        return text;
    }

    /**
     * @param text sets the {@link #text}
     */
    public final void setText(String text)
    {
        this.text = text;
    }

    /**
     * @return {@link #childNotifications}
     */
    public final List<Notification> getChildNotifications()
    {
        return childNotifications;
    }

    /**
     * @param childNotification to be added to the {@link #childNotifications}
     */
    public final void addChildNotification(Notification childNotification)
    {
        childNotifications.add(childNotification);
    }
}
