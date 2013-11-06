package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.util.PasswordAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Helper object for sending emails.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class EmailSender
{
    private static Logger logger = LoggerFactory.getLogger(EmailSender.class);

    /**
     * Sender email address.
     */
    private String sender = null;

    /**
     * Subject prefix.
     */
    private String subjectPrefix = null;

    /**
     * Session for sending emails.
     */
    private Session session;

    /**
     * Constructor.
     *
     * @param configuration from which the SMTP configuration should be loaded
     */
    public EmailSender(ControllerConfiguration configuration)
    {
        // Skip configuration without host
        if (!configuration.containsKey(ControllerConfiguration.SMTP_HOST)) {
            logger.warn("Cannot initialize email notifications because SMTP configuration is empty.");
            return;
        }

        String port = configuration.getString(ControllerConfiguration.SMTP_PORT);
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", configuration.getString(ControllerConfiguration.SMTP_HOST));
        properties.setProperty("mail.smtp.port", port);
        if (!port.equals("25")) {
            properties.setProperty("mail.smtp.starttls.enable", "true");
            properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.setProperty("mail.smtp.socketFactory.fallback", "false");
        }

        sender = configuration.getString(ControllerConfiguration.SMTP_SENDER);
        subjectPrefix = configuration.getString(ControllerConfiguration.SMTP_SUBJECT_PREFIX);

        Authenticator authenticator = null;
        if (configuration.containsKey(ControllerConfiguration.SMTP_USERNAME)) {
            properties.setProperty("mail.smtp.auth", "true");
            authenticator = new PasswordAuthenticator(
                    configuration.getString(ControllerConfiguration.SMTP_USERNAME),
                    configuration.getString(ControllerConfiguration.SMTP_PASSWORD));
        }

        session = Session.getDefaultInstance(properties, authenticator);
    }

    /**
     * @return true whether SMTP is configured,
     *         false otherwise
     */
    public boolean isInitialized()
    {
        return session != null;

    }

    /**
     * Send email.
     *
     * @param recipient
     * @param subject
     * @param content
     * @throws MessagingException
     */
    public void sendEmail(String recipient, String subject, String content) throws MessagingException
    {
        List<String> recipients = new LinkedList<String>();
        recipients.add(recipient);
        sendEmail(recipients, subject, content);
    }

    /**
     * Send email.
     *
     * @param recipients
     * @param subject
     * @param content
     * @throws MessagingException
     */
    public void sendEmail(Collection<String> recipients, String subject, String content) throws MessagingException
    {
        if (session == null) {
            throw new IllegalStateException("Email sender is not initialized.");
        }

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent(content, "text/plain; charset=utf-8");

        StringBuilder html = new StringBuilder();
        html.append("<html><body><pre>");
        html.append(content);
        html.append("</pre></body></html>");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(html.toString(), "text/html; charset=utf-8");

        Multipart multipart = new MimeMultipart("alternative");
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(htmlPart);

        sendEmail(recipients, subject, multipart);
    }

    /**
     * Send email.
     *
     * @param recipients
     * @param subject
     * @param content
     * @throws MessagingException
     */
    public void sendEmail(Collection<String> recipients, String subject, Multipart content) throws MessagingException
    {
        if (session == null) {
            return;
        }
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(sender));
        for (String recipient : recipients) {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        }
        message.setSubject(subjectPrefix + subject);
        message.setContent(content);
        sendEmail(message, subject);
    }

    /**
     * Send email.
     *
     * @param message
     * @throws MessagingException
     */
    private void sendEmail(MimeMessage message, String subject) throws MessagingException
    {
        StringBuilder recipientString = new StringBuilder();
        for (Address recipient : message.getRecipients(Message.RecipientType.TO)) {
            if (recipientString.length() > 0) {
                recipientString.append(", ");
            }
            recipientString.append(recipient.toString());
        }

        logger.debug("Sending email '{}' from '{}' to '{}'...", new Object[]{subject, sender, recipientString});
        Transport.send(message);
    }

}
