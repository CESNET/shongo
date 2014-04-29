package cz.cesnet.shongo.client.web;

/**
 * Can be thrown to show message to the user.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserMessageException extends RuntimeException
{
    /**
     * Title bundle code.
     */
    private final String titleCode;

    /**
     * Message bundle code.
     */
    private final String messageCode;

    /**
     * Constructor.
     *
     * @param titleCode sets the {@link #titleCode}
     * @param messageCode sets the {@link #messageCode}
     */
    public UserMessageException(String titleCode, String messageCode)
    {
        super(messageCode);
        this.titleCode = titleCode;
        this.messageCode = messageCode;
    }

    /**
     * Constructor.
     *
     * @param messageCode sets the {@link #messageCode}
     */
    public UserMessageException(String messageCode)
    {
        this("views.userMessage.titleError", messageCode);
    }

    /**
     * @return {@link #titleCode}
     */
    public String getTitleCode()
    {
        return titleCode;
    }

    /**
     * @return {@link #messageCode}
     */
    public String getMessageCode()
    {
        return messageCode;
    }
}
