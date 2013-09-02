package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.authorization.Authorization;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.*;

/**
 * Represents a notification.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class Notification
{
    protected static Logger logger = LoggerFactory.getLogger(NotificationManager.class);

    /**
     * Notification recipients. Each group should be notified separately.
     */
    private Map<RecipientGroup, Set<PersonInformation>> recipientsByGroup =
            new HashMap<RecipientGroup, Set<PersonInformation>>();

    /**
     * @return {@link #recipientsByGroup}
     */
    public Map<RecipientGroup, Set<PersonInformation>> getRecipientsByGroup()
    {
        return Collections.unmodifiableMap(recipientsByGroup);
    }

    /**
     * @return all recipients from {@link #recipientsByGroup}
     */
    public Collection<PersonInformation> getRecipients()
    {
        List<PersonInformation> recipients = new LinkedList<PersonInformation>();
        for (Set<PersonInformation> recipientsInGroup : recipientsByGroup.values()) {
            recipients.addAll(recipientsInGroup);
        }
        return recipients;
    }

    /**
     * Remove all added {@link #recipientsByGroup}.
     */
    public void clearRecipients()
    {
        recipientsByGroup.clear();
    }

    /**
     * @param recipient to be added to the {@link #recipientsByGroup}
     */
    public void addRecipient(RecipientGroup recipientGroup, PersonInformation recipient)
    {
        Set<PersonInformation> recipients = recipientsByGroup.get(recipientGroup);
        if (recipients == null) {
            recipients = new HashSet<PersonInformation>();
            recipientsByGroup.put(recipientGroup, recipients);
        }
        recipients.add(recipient);
    }

    /**
     * @param recipients to be added to the {@link #recipientsByGroup}
     */
    public void addRecipients(RecipientGroup recipientGroup, Collection<PersonInformation> recipients)
    {
        for (PersonInformation recipient : recipients) {
            addRecipient(recipientGroup, recipient);
        }
    }

    /**
     * @param userId for user to be added to the {@link #recipientsByGroup}
     */
    public void addUserRecipient(String userId)
    {
        try {
            UserInformation userRecipient = Authorization.getInstance().getUserInformation(userId);
            addRecipient(RecipientGroup.USER, userRecipient);
        } catch (ControllerReportSet.UserNotExistException exception) {
            logger.error("User '{}' doesn't exist.", userId);
        }
    }

    /**
     * @return string name of the {@link Notification}
     */
    public abstract String getName();

    /**
     * @return string content of the {@link Notification}
     */
    public abstract String getContent();

    /**
     * @return instance of {@link NotificationTemplateHelper} which will be added as "template" to velocity
     */
    public NotificationTemplateHelper getTemplateHelper()
    {
        return new NotificationTemplateHelper();
    }

    /**
     * Render given {@code notificationTemplateFileName} with specified {@code parameters}.
     *
     * @param notificationTemplateFileName to be rendered
     * @param parameters                   to be rendered
     * @return rendered string
     */
    public String renderTemplate(String notificationTemplateFileName, Map<String, Object> parameters)
    {
        try {
            Template template = getConfiguration().getTemplate("notification/" + notificationTemplateFileName);

            StringWriter stringWriter = new StringWriter();
            parameters.put("template", getTemplateHelper());
            template.process(parameters, stringWriter);
            return stringWriter.toString();
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Single instance of {@link Configuration}.
     */
    private static Configuration configuration;

    /**
     * @return {@link #configuration}
     */
    private static Configuration getConfiguration()
    {
        if (configuration == null) {
            configuration = new Configuration();
            configuration.setObjectWrapper(new DefaultObjectWrapper());
            configuration.setClassForTemplateLoading(Notification.class, "/");
        }
        return configuration;
    }

    /**
     * Enumeration of recipient groups. Each group is notified separately.
     */
    public enum RecipientGroup
    {
        /**
         * Group of Shongo users.
         */
        USER,

        /**
         * Group of Shongo administrators.
         */
        ADMINISTRATOR
    }
}
