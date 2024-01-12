package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.util.PasswordAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.util.*;
import java.util.List;

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
     * @param sender
     * @param subjectPrefix
     */
    public EmailSender(String sender, String subjectPrefix)
    {
        this.sender = sender;
        this.subjectPrefix = subjectPrefix;
        if (this.subjectPrefix == null) {
            this.subjectPrefix = "";
        }
    }

    /**
     * Constructor.
     *
     * @param sender
     * @param smtpHost
     * @param smtpPort
     * @param smtpUsername
     * @param smtpPassword
     */
    public EmailSender(String sender, String smtpHost, int smtpPort, String smtpUsername, String smtpPassword)
    {
        this(sender, null);

        initSession(smtpHost, smtpPort, smtpUsername, smtpPassword);
    }

    /**
     * Constructor.
     *
     * @param smtpHost
     * @param smtpPort
     * @param smtpUsername
     * @param smtpPassword
     */
    public EmailSender(String smtpHost, int smtpPort, String smtpUsername, String smtpPassword)
    {
        this("no-reply@shongo.cz", null);

        initSession(smtpHost, smtpPort, smtpUsername, smtpPassword);
    }

    /**
     * Constructor.
     *
     * @param configuration from which the SMTP configuration should be loaded
     */
    public EmailSender(ControllerConfiguration configuration)
    {
        this(configuration.getString(ControllerConfiguration.SMTP_SENDER),
                configuration.getSmtpSubjectPrefix());

        // Skip configuration without host
        if (!configuration.containsKey(ControllerConfiguration.SMTP_HOST)) {
            logger.warn("Cannot initialize email notifications because SMTP configuration is empty.");
            return;
        }

        String smtpHost = configuration.getString(ControllerConfiguration.SMTP_HOST);
        int smtpPort = configuration.getInt(ControllerConfiguration.SMTP_PORT);
        String smtpUsername = null;
        String smtpPassword = null;
        if (configuration.containsKey(ControllerConfiguration.SMTP_USERNAME)) {
            smtpUsername = configuration.getString(ControllerConfiguration.SMTP_USERNAME);
            smtpPassword = configuration.getString(ControllerConfiguration.SMTP_PASSWORD);
        }
        initSession(smtpHost, smtpPort, smtpUsername, smtpPassword);
    }

    /**
     * @return {@link #sender}
     */
    public String getSender()
    {
        return sender;
    }

    /**
     * Initialize SMTP session.
     *
     * @param smtpHost
     * @param smtpPort
     * @param smtpUserName
     * @param smtpPassword
     */
    private void initSession(String smtpHost, int smtpPort, String smtpUserName, String smtpPassword)
    {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", smtpHost);
        properties.setProperty("mail.smtp.port", String.valueOf(smtpPort));
        if (smtpPort != 25) {
            properties.setProperty("mail.smtp.starttls.enable", "true");
            properties.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.setProperty("mail.smtp.socketFactory.fallback", "false");
        }
        Authenticator authenticator = null;
        if (smtpUserName != null) {
            properties.setProperty("mail.smtp.auth", "true");
            authenticator = new PasswordAuthenticator(smtpUserName, smtpPassword);
        }
        session = Session.getDefaultInstance(properties, authenticator);
    }

    /**
     * @return true whether SMTP is configured,
     * false otherwise
     */
    public boolean isInitialized()
    {
        return session != null;
    }

    /**
     * Send email.
     *
     * @param email
     * @throws MessagingException
     */
    public void sendEmail(Email email) throws MessagingException
    {
        if (session == null) {
            return;
        }
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(sender));
        for (InternetAddress recipient : email.recipients) {
            message.addRecipient(Message.RecipientType.TO, recipient);
        }
        if (email.replyTo.size() > 0) {
            message.setReplyTo(email.replyTo.toArray(new Address[email.replyTo.size()]));
        }
        message.setSubject(subjectPrefix + email.subject, "utf-8");
        message.addHeader("Precedence","bulk");

        // Create content multipart
        MimeMultipart contentMultipart = new MimeMultipart("alternative");

        // Add text part
        String content = email.content;
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent(content, "text/plain; charset=utf-8");
        contentMultipart.addBodyPart(textPart);

        // Add html part
        String htmlContent = "<html><body><pre>" + content + "</pre></body></html>";
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlContent, "text/html; charset=utf-8");
        contentMultipart.addBodyPart(htmlPart);

        MimeMultipart messageMultipart;
        if (email.attachments.size() > 0) {
            // Create content multipart part
            MimeBodyPart contentMultipartPart = new MimeBodyPart();
            contentMultipartPart.setContent(contentMultipart);
            // Create message multipart
            messageMultipart = new MimeMultipart("related");
            messageMultipart.addBodyPart(contentMultipartPart);
            // Add attachments
            for (Map.Entry<String, DataSource> entry : email.attachments.entrySet()) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.setDataHandler(new DataHandler(entry.getValue()));
                attachmentPart.setFileName(entry.getKey());
                messageMultipart.addBodyPart(attachmentPart);
            }
        }
        else {
            // Content multipart is used as message multipart
            messageMultipart = contentMultipart;
        }

        // Set message multipart content
        message.setContent(messageMultipart);

        StringBuilder recipientString = new StringBuilder();
        Address[] recipients = message.getRecipients(Message.RecipientType.TO);
        if (recipients != null) {
            for (Address recipient : recipients) {
                if (recipientString.length() > 0) {
                    recipientString.append(", ");
                }
                recipientString.append(recipient.toString());
            }
        }
        logger.debug("Sending email '{}' from '{}' to '{}'...", new Object[]{email.subject, sender, recipientString});
        Transport.send(message);
    }

    public static class Email
    {
        private List<InternetAddress> recipients = new LinkedList<InternetAddress>();

        private List<InternetAddress> replyTo = new LinkedList<InternetAddress>();

        private String subject;

        private String content;

        private Map<String, DataSource> attachments = new LinkedHashMap<String, DataSource>();

        public Email(String recipient, String subject, String content)
        {
            addRecipient(recipient);
            setSubject(subject);
            setContent(content);
        }

        public Email(Collection<String> recipients, String subject, String content)
        {
            addRecipients(recipients);
            setSubject(subject);
            setContent(content);
        }

        public void addRecipients(Collection<String> recipients) {
            for (String recipient : recipients) {
                addRecipient(recipient);
            }
        }

        public Email(String recipient, Collection<String> replyTo, String subject, String content)
        {
            addRecipient(recipient);
            for (String replyToItem : replyTo) {
                addReplyTo(replyToItem);
            }
            setSubject(subject);
            setContent(content);
        }

        public Email(Collection<String> recipient, String replyTo, String subject, String content)
        {
            addRecipients(recipient);
            addReplyTo(replyTo);
            setSubject(subject);
            setContent(content);
        }

        public void addRecipient(String recipient)
        {
            try {
                recipients.add(new InternetAddress(recipient));
            }
            catch (AddressException exception) {
                throw new IllegalArgumentException(exception);
            }
        }

        public void addReplyTo(String replyTo)
        {
            try {
                this.replyTo.add(new InternetAddress(replyTo));
            }
            catch (AddressException exception) {
                throw new IllegalArgumentException(exception);
            }
        }

        public String getSubject()
        {
            return subject;
        }

        public void setSubject(String subject)
        {
            this.subject = subject;
        }

        public void setContent(String content)
        {
            if (content == null) {
                content = "";
            }
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public void addCalendarAttachment(String fileName, String fileContent)
        {
            try {
                DataSource dataSource = new ByteArrayDataSource(fileContent, "text/calendar; charset=UTF-8");
                attachments.put(fileName, dataSource);
            }
            catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        public void addPdfAttachment(String fileName, byte[] fileContent)
        {
            DataSource dataSource = new ByteArrayDataSource(fileContent, "application/pdf; charset=UTF-8");
            attachments.put(fileName, dataSource);

        }
    }

}
