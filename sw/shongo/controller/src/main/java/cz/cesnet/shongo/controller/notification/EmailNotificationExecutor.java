package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.report.InternalErrorHandler;
import cz.cesnet.shongo.controller.report.InternalErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * {@link NotificationExecutor} for sending mails.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class EmailNotificationExecutor extends NotificationExecutor
{
    private static Logger logger = LoggerFactory.getLogger(EmailNotificationExecutor.class);

    private static final String EMAIL_HEADER = ""
            + "===========================================================\n"
            + " Automatic notification from the Shongo reservation system \n"
            + "===========================================================\n\n";
    /**
     * Sender email address.
     */
    private String emailSender = null;

    /**
     * Subject prefix.
     */
    private String subjectPrefix = null;

    /**
     * Session for sending emails.
     */
    private Session session;

    @Override
    public void init(Configuration configuration)
    {
        super.init(configuration);

        // Skip configuration without host
        if (!configuration.containsKey(Configuration.SMTP_HOST)) {
            logger.warn("Cannot initialize email notifications because SMTP configuration is empty.");
            return;
        }

        String port = configuration.getString(Configuration.SMTP_PORT);
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", configuration.getString(Configuration.SMTP_HOST));
        properties.setProperty("mail.smtp.port", port);
        if (!port.equals("25")) {
            properties.setProperty("mail.smtp.starttls.enable", "true");
            properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.setProperty("mail.smtp.socketFactory.fallback", "false");
        }

        emailSender = configuration.getString(Configuration.SMTP_SENDER);
        subjectPrefix = configuration.getString(Configuration.SMTP_SUBJECT_PREFIX);

        Authenticator authenticator = null;
        if (configuration.containsKey(Configuration.SMTP_USERNAME)) {
            properties.setProperty("mail.smtp.auth", "true");
            authenticator = new PasswordAuthenticator(
                    configuration.getString(Configuration.SMTP_USERNAME),
                    configuration.getString(Configuration.SMTP_PASSWORD));
        }

        session = Session.getDefaultInstance(properties, authenticator);
    }

    @Override
    public void executeNotification(Notification notification)
    {
        if (session == null) {
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

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(text.toString(), "text/plain; charset=utf-8");

            StringBuilder html = new StringBuilder();
            html.append("<html><body><pre>");
            html.append(text);
            html.append("</pre></body></html>");

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(html.toString(), "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart("alternative");
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(htmlPart);

            MimeMessage message = new MimeMessage(session);

            StringBuilder recipientString = new StringBuilder();
            for (String recipient : recipients) {
                if (recipientString.length() > 0) {
                    recipientString.append(", ");
                }
                recipientString.append(recipient);
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }

            message.setFrom(new InternetAddress(emailSender));
            message.setSubject(subjectPrefix + notification.getName());
            message.setContent(multipart);

            logger.debug("Sending email '{}' from '{}' to '{}'...\n",
                    new Object[]{message.getSubject(), emailSender, recipientString});
            sendMail(message);
        }
        catch (Exception exception) {
            InternalErrorHandler.handle(InternalErrorType.NOTIFICATION, "Failed to send email", exception);
        }
    }

    /**
     * Send email.
     *
     * @param message
     * @throws MessagingException
     */
    protected void sendMail(Message message) throws MessagingException
    {
        Transport.send(message);
    }

    /**
     * {@link Authenticator} for username and password.
     */
    private static class PasswordAuthenticator extends Authenticator
    {
        /**
         * Username.
         */
        private String userName;

        /**
         * Password for {@link #userName}.
         */
        private String password;

        /**
         * Constructor.
         *
         * @param userName sets the {@link #userName}
         * @param password sets the {@link #password}
         */
        public PasswordAuthenticator(String userName, String password)
        {
            this.userName = userName;
            this.password = password;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication()
        {
            return new PasswordAuthentication(userName, password);
        }
    }
}
