package cz.cesnet.shongo.controller.notification;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NotificationMessage
{
    private String name;

    private StringBuilder content = new StringBuilder();

    public NotificationMessage()
    {
    }

    public NotificationMessage(String name, String content)
    {
        this.name = name;
        this.content.append(content);
    }

    public String getName()
    {
        return name;
    }

    public String getContent()
    {
        return content.toString();
    }

    public void appendMessage(NotificationMessage configurationMessage)
    {
        if (name == null) {
            name = configurationMessage.getName();
        }
        if (content.length() > 0) {
            content.append("\n\n");
        }
        content.append(configurationMessage.getContent());
    }
}
