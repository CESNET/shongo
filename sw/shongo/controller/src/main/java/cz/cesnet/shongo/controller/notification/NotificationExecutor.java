package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.util.TemporalHelper;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Represent an abstract executor of {@link Notification}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class NotificationExecutor
{
    protected static Logger logger = LoggerFactory.getLogger(NotificationExecutor.class);

    /**
     * Header for notifications converted to string.
     */
    private static final String HEADER = ""
            + "=========================================================================\n"
            + "        Automatic notification from the Shongo reservation system        \n"
            + "=========================================================================\n";

    /**
     * @param notification to be executed
     */
    public abstract void executeNotification(Notification notification);

    /**
     * Initialize {@link NotificationExecutor}.
     *
     * @param configuration from which the {@link NotificationExecutor} can load settings
     */
    public void init(Configuration configuration)
    {
    }

    /**
     * @return given {@code notification} converted to string
     */
    protected String getNotificationAsString(Notification notification)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(HEADER);
        stringBuilder.append("\n");
        stringBuilder.append(getNotificationAsString(notification, false));
        return stringBuilder.toString();
    }

    /**
     * @param notification to be converted to string
     * @param child        specifies whether the resulting string is used inside
     *                     another {@link #getNotificationAsString(Notification, boolean)} call
     * @return given {@code notification} converted to string
     */
    private String getNotificationAsString(Notification notification, boolean child)
    {
        StringBuilder stringBuilder = new StringBuilder();
        if (notification.getText() != null) {
            stringBuilder.append(notification.getText());
        }
        if (notification instanceof ObjectNotification) {
            String objectNotificationContent = getObjectNotificationContent((ObjectNotification) notification, child);
            stringBuilder.append(objectNotificationContent);
        }

        for (Notification childNotification : notification.getChildNotifications()) {
            String childNotificationString = getNotificationAsString(childNotification, child);
            stringBuilder.append("\n");
            stringBuilder.append(childNotificationString.trim());
        }

        stringBuilder.append("\n");

        return stringBuilder.toString();
    }

    /**
     * @param objectNotification to be converted to string
     * @param child              specifies whether the resulting string is used inside
     *                           another {@link #getNotificationAsString(Notification, boolean)} call
     * @return given {@code objectNotification}'s content converted to string
     */
    private String getObjectNotificationContent(ObjectNotification objectNotification, boolean child)
    {
        StringBuilder stringBuilder = new StringBuilder();

        // Append name
        String name = objectNotification.getName();
        if (name != null && !child) {
            stringBuilder.append(name);
            stringBuilder.append(":\n");
            int nameLength = name.length() + 1;
            for (int index = 0; index < nameLength; index++) {
                stringBuilder.append("-");
            }
        }

        // Append properties
        List<String> propertyNames = objectNotification.getPropertyNames();
        if (propertyNames.size() > 0) {
            // Calculate maximum length of property name
            int maxPropertyLengthWidth = 0;
            for (String propertyName : propertyNames) {
                Object propertyValue = objectNotification.getPropertyValue(propertyName);
                if (propertyValue == null || propertyValue instanceof Notification) {
                    continue;
                }
                int propertyNameLength = propertyName.length();
                if (propertyNameLength > maxPropertyLengthWidth) {
                    maxPropertyLengthWidth = propertyNameLength;
                }
            }

            // Format and append properties
            String propertyIndent = "  ";
            String propertyNameFormat = String.format("%s%%-%ds  %%s", propertyIndent, maxPropertyLengthWidth + 1);
            int propertyValueNewLineIndent = String.format(propertyNameFormat, propertyNames.get(0), "").length();
            String propertyValueNewLine = String.format(String.format("\n%%%ds", propertyValueNewLineIndent), "");
            String propertySeparator = "\n\n";
            if (child) {
                propertySeparator = "\n";
            }

            for (String propertyName : propertyNames) {
                Object propertyValue = objectNotification.getPropertyValue(propertyName);
                if (propertyValue == null) {
                    continue;
                }
                String propertyValueString;
                if (propertyValue instanceof ObjectNotification) {
                    Notification notification = (Notification) propertyValue;
                    propertyValueString = getNotificationAsString(notification, true);
                    propertyValueString = propertyValueString.replace("\n", "\n" + propertyIndent);
                }
                else if (propertyValue instanceof DateTime) {
                    propertyValueString = TemporalHelper.formatDateTime((DateTime) propertyValue);
                }
                else if (propertyValue instanceof Period) {
                    propertyValueString = TemporalHelper.formatPeriod((Period) propertyValue);
                }
                else if (propertyValue instanceof Interval) {
                    propertyValueString = TemporalHelper.formatInterval((Interval) propertyValue);
                }
                else {
                    propertyValueString = propertyValue.toString();
                    propertyValueString = propertyValueString.replace("\n", propertyValueNewLine);
                }
                String property = String.format(propertyNameFormat, propertyName + ":", propertyValueString);
                stringBuilder.append(propertySeparator);
                stringBuilder.append(property);
            }
        }
        return stringBuilder.toString();
    }
}
