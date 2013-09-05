package cz.cesnet.shongo.controller.notification;

/**
 * Rendered message from {@link Notification} for a single recipient.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NotificationMessage
{
    private static final String SEPARATOR =
            "\n\n--------------------------------------------------------------------------------\n\n";

    private String title;

    private StringBuilder content = new StringBuilder();

    public NotificationMessage()
    {
    }

    public NotificationMessage(String title, String content)
    {
        this.title = title;
        this.content.append(content);
    }

    public String getTitle()
    {
        return title;
    }

    public String getContent()
    {
        return content.toString();
    }

    public void appendMessage(NotificationMessage configurationMessage)
    {
        if (title == null) {
            title = configurationMessage.getTitle();
        }
        if (content.length() > 0) {
            content.append(SEPARATOR);
        }
        content.append(configurationMessage.getContent());
    }

    public void appendChildMessage(NotificationMessage configurationMessage)
    {
        if (content.length() > 0) {
            content.append("\n\n");
        }
        content.append(configurationMessage.getTitle());
        content.append("\n");
        content.append(configurationMessage.getTitle().replaceAll(".", "-"));
        content.append("\n");
        content.append(configurationMessage.getContent());
    }
}
