package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.EmailSender;
import cz.cesnet.shongo.controller.Reporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        StringBuilder text = new StringBuilder();
        text.append(EMAIL_HEADER);
        text.append(notification.getContent());

        Map<Notification.RecipientGroup, Set<PersonInformation>> recipients =  notification.getRecipientsByGroup();
        for (Notification.RecipientGroup recipientGroup : recipients.keySet()) {
            Set<String> recipientEmails = new HashSet<String>();
            for (PersonInformation recipient : recipients.get(recipientGroup)) {
                String email = recipient.getPrimaryEmail();
                if (email != null) {
                    recipientEmails.add(email);
                }
            }
            if (recipients.size() == 0) {
                logger.warn("Notification '{}' doesn't have any recipients with email address.", notification.getName());
                return;
            }

            try {
                emailSender.sendEmail(recipientEmails, notification.getName(), text.toString());
            }
            catch (Exception exception) {
                Reporter.reportInternalError(Reporter.InternalErrorType.NOTIFICATION, "Failed to send email", exception);
            }
        }
    }
}
