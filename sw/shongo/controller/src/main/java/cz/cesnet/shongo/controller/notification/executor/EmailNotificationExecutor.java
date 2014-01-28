package cz.cesnet.shongo.controller.notification.executor;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.EmailSender;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.api.UserSettings;
import cz.cesnet.shongo.controller.notification.AbstractNotification;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import cz.cesnet.shongo.controller.notification.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    private static final Map<String, String> EMAIL_HEADER = new HashMap<String, String>()
    {{
            put(UserSettings.LOCALE_ENGLISH.getLanguage(), "Automatic notification from the reservation system");
            put(UserSettings.LOCALE_CZECH.getLanguage(), "Automatická zpráva z rezervačního systému");
        }};

    /**
     * @see EmailSender
     */
    private EmailSender emailSender;

    /**
     * @see ControllerConfiguration
     */
    private ControllerConfiguration configuration;

    /**
     * Constructor.
     *
     * @param emailSender sets the {@link #emailSender}
     */
    public EmailNotificationExecutor(EmailSender emailSender, ControllerConfiguration configuration)
    {
        this.emailSender = emailSender;
        this.configuration = configuration;
    }

    @Override
    public boolean executeNotification(PersonInformation recipient, AbstractNotification notification,
            NotificationManager manager, EntityManager entityManager)
    {
        if (!emailSender.isInitialized()) {
            return false;
        }
        try {
            String recipientEmail = recipient.getPrimaryEmail();
            if (recipientEmail == null) {
                logger.warn("Notification '{}' has empty email address.", notification);
                return false;
            }
            List<String> replyToEmails = new LinkedList<String>();
            for (PersonInformation replyTo : notification.getReplyTo()) {
                String replyToEmail = replyTo.getPrimaryEmail();
                if (replyToEmail != null) {
                    replyToEmails.add(replyToEmail);
                }
            }
            NotificationMessage message = notification.getMessage(recipient, manager, entityManager);

            // Build email header
            StringBuilder emailHeaderBuilder = new StringBuilder();
            for (String language : message.getLanguages()) {
                String emailHeader = EMAIL_HEADER.get(language);
                if (emailHeader != null) {
                    if (emailHeaderBuilder.length() > 0) {
                        emailHeaderBuilder.append(" / ");
                    }
                    emailHeaderBuilder.append(emailHeader);
                }
            }
            emailHeaderBuilder.insert(0, " ");
            emailHeaderBuilder.append(" ");
            String emailHeader = emailHeaderBuilder.toString();
            String emailHeaderLine = emailHeader.replaceAll(".", "=");
            emailHeaderBuilder = new StringBuilder();
            emailHeaderBuilder.append(emailHeaderLine);
            emailHeaderBuilder.append("\n");
            emailHeaderBuilder.append(emailHeader);
            emailHeaderBuilder.append("\n");
            emailHeaderBuilder.append(emailHeaderLine);
            emailHeaderBuilder.append("\n\n");

            // Build email content
            StringBuilder emailContent = new StringBuilder();
            emailContent.append(emailHeaderBuilder.toString());
            emailContent.append(message.getContent());

            // Send email
            emailSender.sendEmail(recipientEmail, replyToEmails, message.getTitle(), emailContent.toString());
            return true;
        }
        catch (Exception exception) {
            Reporter.reportInternalError(Reporter.NOTIFICATION, "Failed to send email", exception);
            return false;
        }
    }
}
