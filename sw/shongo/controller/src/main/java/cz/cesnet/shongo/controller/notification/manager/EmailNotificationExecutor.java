package cz.cesnet.shongo.controller.notification.manager;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.EmailSender;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.notification.Notification;
import cz.cesnet.shongo.controller.notification.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link NotificationExecutor} for sending mails.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class EmailNotificationExecutor extends NotificationExecutor
{
    private static Logger logger = LoggerFactory.getLogger(EmailNotificationExecutor.class);

    /**
     * Header for email notifications.
     */
    private static final String EMAIL_HEADER = ""
            + "===========================================================\n"
            + " Automatic notification from the Shongo reservation system \n"
            + "===========================================================\n\n";

    /**
     * @see EmailSender
     */
    private EmailSender emailSender;

    /**
     * Constructor.
     *
     * @param emailSender sets the {@link #emailSender}
     */
    public EmailNotificationExecutor(EmailSender emailSender)
    {
        this.emailSender = emailSender;
    }

    @Override
    public void executeNotification(Notification notification)
    {
        if (!emailSender.isInitialized()) {
            return;
        }
        for (PersonInformation recipient : notification.getRecipients()) {
            NotificationMessage recipientMessage = notification.getRecipientMessage(recipient);
            String recipientEmail = recipient.getPrimaryEmail();
            if (recipientEmail == null) {
                logger.warn("Recipient '{}' for notification '{}' has empty email address.",
                        recipient, recipientMessage.getTitle());
                return;
            }
            try {
                StringBuilder recipientMessageContent = new StringBuilder();
                recipientMessageContent.append(EMAIL_HEADER);
                recipientMessageContent.append(recipientMessage.getContent());
                emailSender.sendEmail(recipientEmail, recipientMessage.getTitle(), recipientMessageContent.toString());
            }
            catch (Exception exception) {
                Reporter.reportInternalError(Reporter.NOTIFICATION, "Failed to send email", exception);
            }
        }
    }
}
