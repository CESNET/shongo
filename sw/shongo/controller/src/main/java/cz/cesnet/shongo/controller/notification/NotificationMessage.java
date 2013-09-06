package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.controller.api.UserSettings;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * Rendered message from {@link Notification} for a single recipient.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NotificationMessage
{
    /**
     * Available {@link java.util.Locale}s for {@link Notification}s.
     */
    public static List<Locale> AVAILABLE_LOCALES = new LinkedList<Locale>(){{
        add(UserSettings.LOCALE_ENGLISH);
        add(UserSettings.LOCALE_CZECH);
    }};

    /**
     * Languag string for each {@link #AVAILABLE_LOCALES}.
     */
    public static Map<String, String> LANGUAGE_STRING = new HashMap<String, String>() {{
        put(UserSettings.LOCALE_ENGLISH.getLanguage(), "ENGLISH VERSION");
        put(UserSettings.LOCALE_CZECH.getLanguage(), "ČESKÁ VERZE");
    }};

    private String title;

    private StringBuilder content = new StringBuilder();

    public NotificationMessage()
    {
    }

    public NotificationMessage(String title, String content)
    {
        this.title = title;
        this.content.append(StringUtils.stripEnd(content, null));
    }

    public String getTitle()
    {
        return title;
    }

    public String getContent()
    {
        return content.toString();
    }

    public void appendMessage(NotificationMessage message, ConfigurableNotification.Configuration configuration)
    {
        if (content.length() > 0) {
            content.append("\n\n");
        }

        String languageString = LANGUAGE_STRING.get(configuration.getLocale().getLanguage());
        appendLine(languageString);

        if (title == null) {
            title = message.getTitle();
        }
        else {

            appendLine(message.getTitle());
        }
        content.append("\n");
        content.append(message.getContent());
    }

    public void appendLine(String text)
    {
        int length = 80;
        if (!text.isEmpty()) {
            length -= 4 + text.length();
            content.append("-- ");
            content.append(text);
            content.append(" ");
        }
        for (int index = 0; index < length; index++) {
            content.append("-");
        }
        content.append("\n");
    }

    public void appendChildMessage(NotificationMessage configurationMessage)
    {
        if (content.length() > 0) {
            content.append("\n\n");
        }
        String indent = "  ";
        StringBuilder childContent = new StringBuilder();
        childContent.append(indent);
        childContent.append(configurationMessage.getTitle());
        childContent.append("\n");
        childContent.append(configurationMessage.getTitle().replaceAll(".", "-"));
        childContent.append("\n");
        childContent.append(configurationMessage.getContent());
        content.append(childContent.toString().replaceAll("\n", "\n" + indent));
    }
}
