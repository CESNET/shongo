package cz.cesnet.shongo.controller.report;

import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.Controller;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

/**
 * Handler for internal errors.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class InternalErrorHandler
{
    private static Logger logger = LoggerFactory.getLogger(InternalErrorHandler.class);

    /**
     * Handle internal error.
     *
     * @param type
     * @param message
     * @param exception
     */
    public static void handle(InternalErrorType type, String message, Exception exception)
    {
        StringBuilder messageBuilder = new StringBuilder();
        if (type != null) {
            messageBuilder.append(type.getName() + " Error");
        }
        if (message != null) {
            if (messageBuilder.length() > 0) {
                messageBuilder.append(": ");
            }
            messageBuilder.append(message);
        }
        if (messageBuilder.length() == 0) {
            messageBuilder.append("Unknown Error");
        }
        message = messageBuilder.toString();

        // Log error
        logger.error(message, exception);

        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("CONFIGURATION\n\n");

        Domain domain = Domain.getLocalDomain();
        contentBuilder.append("  Domain: ")
                .append(domain.getName())
                .append(" (")
                .append(domain.getOrganization())
                .append(")\n");

        String hostName = Controller.getInstance().getRpcHost();
        if (hostName.isEmpty()) {
            try {
                hostName = java.net.InetAddress.getLocalHost().getHostName();
            }
            catch (UnknownHostException unknownHostException) {
                logger.error("Cannot get local hostname.", unknownHostException);
            }
        }
        contentBuilder.append("  Host:   ")
                .append(hostName)
                .append("\n");

        if (exception != null) {
            contentBuilder.append("\nEXCEPTION\n\n");
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            exception.printStackTrace(printWriter);
            String stackTrace = result.toString();
            contentBuilder.append(stackTrace);
        }

        // Send error to administrators
        EmailSender emailSender = Controller.getInstance().getEmailSender();
        try {
            emailSender.sendEmail(getAdministratorEmails(), message, contentBuilder.toString());
        }
        catch (MessagingException messagingException) {
            logger.error("Failed sending error email.", messagingException);
        }
    }

    /**
     * Handle internal error.
     *
     * @param type
     * @param message
     */
    public static void handle(InternalErrorType type, String message)
    {
        handle(type, message, null);
    }

    /**
     * Handle internal error.
     *
     * @param type
     * @param exception
     */
    public static void handle(InternalErrorType type, Exception exception)
    {
        handle(type, null, exception);
    }

    /**
     * @return list of administrator email addresses
     */
    private static List<String> getAdministratorEmails()
    {
        List<String> administratorEmails = new LinkedList<String>();
        Configuration configuration = Controller.getInstance().getConfiguration();
        for (Object item : configuration.getList(Configuration.ADMINISTRATOR_EMAIL)) {
            administratorEmails.add((String) item);
        }
        return administratorEmails;
    }
}
