package cz.cesnet.shongo.controller.util;

import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;

import javax.mail.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Utility class for sending emails.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class MailSender extends Component
{
    /**
     * Sender email address.
     */
    private static final String FROM_SHONGO = "info@shongo.cz";

    /**
     * Header for all messages
     */
    private static final String HEADER = "\n"
            + "==================================================================\n"
            + "        Automatic email from the Shongo reservation system        \n"
            + "==================================================================\n";

    /**
     * Session for sending emails.
     */
    private Session session;

    public MailSender()
    {
    }

    @Override
    public void init(Configuration configuration)
    {
        super.init(configuration);

        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", configuration.getString(Configuration.DOMAIN_ORGANIZATION));

        // Get the default Session object.
        session = Session.getDefaultInstance(properties);
    }

    /**
     * @param mail to be send
     */
    public void send(Mail mail)
    {
    }

    /**
     * Represents an email.
     */
    public static class Mail
    {
        /**
         * List of recipients of the email.
         */
        private List<String> recipients = new ArrayList<String>();

        /**
         * Subject of the email.
         */
        private String subject;

        /**
         * Text of the email.
         */
        private String text;

        public List<String> getRecipients()
        {
            return recipients;
        }

        /**
         * @param recipient to be added to the {@link #recipients}
         */
        public void addRecipient(String recipient)
        {
            recipients.add(recipient);
        }

        /**
         * @return {@link #subject}
         */
        public String getSubject()
        {
            return subject;
        }

        /**
         * @param subject sets the {@link #subject}
         */
        public void setSubject(String subject)
        {
            this.subject = subject;
        }

        /**
         * @return {@link #text}
         */
        public String getText()
        {
            return text;
        }

        /**
         * @param text sets the {@link #text}
         */
        public void setText(String text)
        {
            this.text = text;
        }
    }
}
