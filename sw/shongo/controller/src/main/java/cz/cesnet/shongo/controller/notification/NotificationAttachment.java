package cz.cesnet.shongo.controller.notification;

/**
 * Represents an attachment for {@link NotificationMessage}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class NotificationAttachment
{
    /**
     * Attachment file name.
     */
    private final String fileName;

    /**
     * Constructor.
     *
     * @param fileName sets the {@link #fileName}
     */
    public NotificationAttachment(String fileName)
    {
        this.fileName = fileName;
    }

    /**
     * @return {@link #fileName}
     */
    public String getFileName()
    {
        return fileName;
    }
}
