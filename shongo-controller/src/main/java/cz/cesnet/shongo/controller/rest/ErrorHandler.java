package cz.cesnet.shongo.controller.rest;

import cz.cesnet.shongo.controller.Controller;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.util.Collection;

/**
 * Error handler.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @author Filip Karnis
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorHandler
{

    private final Controller controller;
    private final ControllerConfiguration configuration;

    /**
     * Send email to administrators.
     *
     * @param replyTo
     * @param subject
     * @param content
     * @return result
     */
    public void sendEmailToAdministrator(String replyTo, String subject, String content) throws MessagingException
    {
        Collection<String> administratorEmails = configuration.getAdministratorEmails();
        if (administratorEmails.size() == 0) {
            log.warn("Administrator email for sending error reports is not configured.");
            return;
        }

        try {
            EmailSender.Email email = new EmailSender.Email(administratorEmails, replyTo, subject, content);
            controller.getEmailSender().sendEmail(email);
        }
        catch (MessagingException e) {
            log.error("Failed to send email '" + subject + "':\n" + content, e);
            throw e;
        }
    }
}
