package cz.cesnet.shongo.controller.domains;

import com.google.common.base.Strings;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;

/**
 * Domain admin notifier for inter domain communication
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DomainAdminNotifier {
    private final Logger logger;

    private final EmailSender emailSender;

    private final ControllerConfiguration configuration;

    public DomainAdminNotifier(Logger logger, EmailSender emailSender, ControllerConfiguration configuration)
    {
        this.logger = logger;
        if (emailSender == null) {
            logger.warn("Inter domain agent is not set properly, thus will not sent notifications.");
        }
        this.emailSender = emailSender;
        this.configuration = configuration;
    }

    public void notifyDomainAdmins(String message, Throwable exception)
    {
        if (emailSender == null) {
            return;
        }
        if (Strings.isNullOrEmpty(message)) {
            throw new IllegalArgumentException("Message cannot be null or epmty.");
        }
        String subject = "Error in InterDomainAgent";
        if (exception != null) {
            message += "\n";
            message += exception.toString();
        }
        EmailSender.Email emailNotification = new EmailSender.Email(configuration.getAdministratorEmails(), subject, message);
        try {
            emailSender.sendEmail(emailNotification);
        } catch (MessagingException e) {
            logger.error("Failed to send error to domain admins.", e);
        }
    }

    public void logAndNotifyDomainAdmins(String message, Throwable exception)
    {
        logger.error(message, exception);
        notifyDomainAdmins(message, exception);
    }

    public void logAndNotifyDomainAdmins(String message)
    {
        logger.error(message);
        notifyDomainAdmins(message, null);
    }

    public void logDomainAdmins(String message, Throwable exception)
    {
        logger.error(message, exception);
    }
}
