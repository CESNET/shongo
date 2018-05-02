package cz.cesnet.shongo.controller.notification.executor;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.EmailSender;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.api.UserSettings;
import cz.cesnet.shongo.controller.notification.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
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
    public void executeNotification(PersonInformation recipient, AbstractNotification notification,
            NotificationManager manager, EntityManager entityManager)
    {
        if (!emailSender.isInitialized()) {
            return;
        }
        try {
            String recipientEmail = recipient.getPrimaryEmail();
            if (manager.getRedirectTo() != null) {
                PersonInformation redirectTo = manager.getRedirectTo();
                logger.warn("Notification '{}' is redirected to (name: {}, organization: {}, email: {}).", new Object[]{
                        notification,
                        redirectTo.getFullName(), redirectTo.getRootOrganization(), redirectTo.getPrimaryEmail()
                });
                recipientEmail = redirectTo.getPrimaryEmail();
            }
            if (recipientEmail == null) {
                logger.warn("Notification '{}' has empty email address.", notification);
                return;
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

            EmailSender.Email email = new EmailSender.Email(recipientEmail, message.getTitle(), emailContent.toString());
            for (PersonInformation replyTo : notification.getReplyTo()) {
                String replyToEmail = replyTo.getPrimaryEmail();
                if (replyToEmail != null) {
                    email.addReplyTo(replyToEmail);
                }
            }
            for (NotificationAttachment attachment : message.getAttachments()) {
                String fileName = attachment.getFileName();
                String fileContent = null;
                byte[] byteFileContent= null;
                if (attachment instanceof iCalendarNotificationAttachment) {
                    iCalendarNotificationAttachment calendarAttachment = (iCalendarNotificationAttachment) attachment;
                    fileContent = calendarAttachment.getFileContent(emailSender.getSender(), entityManager);
                    email.addAttachment(fileName, fileContent);
                }
                else if (attachment instanceof PdfNotificationAttachment) {
                    PdfNotificationAttachment calendarAttachment = (PdfNotificationAttachment) attachment;
                    try {
                        byteFileContent = calendarAttachment.getFileContent();
                    } catch (IOException e) {
                        Reporter.getInstance().reportInternalError(Reporter.NOTIFICATION, "Failed to read email attachment", e);
                    }
                    email.addPdfAttachment(fileName, byteFileContent);

                } else {
                    throw new TodoImplementException(attachment.getClass());
                }


            }

            // Send email
            emailSender.sendEmail(email);
        }
        catch (Exception exception) {
            Reporter.getInstance().reportInternalError(Reporter.NOTIFICATION, "Failed to send email", exception);
        }
    }
}
