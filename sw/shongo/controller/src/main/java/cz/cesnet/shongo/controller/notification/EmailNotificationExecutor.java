package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.EmailSender;
import cz.cesnet.shongo.controller.report.InternalErrorHandler;
import cz.cesnet.shongo.controller.report.InternalErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
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
        Set<String> recipients = new HashSet<String>();
        for (PersonInformation recipient : notification.getRecipients()) {
            String email = recipient.getPrimaryEmail();
            if (email != null) {
                recipients.add(email);
            }
        }
        if (recipients.size() == 0) {
            logger.warn("Notification '{}' doesn't have any recipients with email address.", notification.getName());
            return;
        }

        try {
            StringBuilder text = new StringBuilder();
            text.append(EMAIL_HEADER);
            text.append(notification.getContent());

            emailSender.sendEmail(recipients, notification.getName(), text.toString());
        }
        catch (Exception exception) {
            InternalErrorHandler.handle(InternalErrorType.NOTIFICATION, "Failed to send email", exception);
        }
    }
}
