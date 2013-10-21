package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.api.UserInformation;
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
     * Language string for each {@link #AVAILABLE_LOCALES}.
     */
    public static Map<String, String> LANGUAGE_STRING = new HashMap<String, String>() {{
        put(UserSettings.LOCALE_ENGLISH.getLanguage(), "ENGLISH VERSION");
        put(UserSettings.LOCALE_CZECH.getLanguage(), "ČESKÁ VERZE");
    }};

    /**
     * Use settings string for each {@link #AVAILABLE_LOCALES}.
     */
    public static Map<String, String> USER_SETTINGS_STRING = new HashMap<String, String>() {{
        put(UserSettings.LOCALE_ENGLISH.getLanguage(), "you can select your preferred language at ");
        put(UserSettings.LOCALE_CZECH.getLanguage(), "preferovaný jazyk si můžete zvolit na ");
    }};

    private String userSettingsUrl;

    private Set<String> languages = new HashSet<String>();

    private String title;

    private StringBuilder content = new StringBuilder();

    public NotificationMessage(PersonInformation recipient, cz.cesnet.shongo.controller.Configuration configuration)
    {
        if (recipient instanceof UserInformation) {
            userSettingsUrl = configuration.getNotificationUserSettingsUrl();
        }
    }

    public NotificationMessage(String language, String title, String content)
    {
        this.languages.add(language);
        this.title = title;
        this.content.append(StringUtils.stripEnd(content, null));
    }

    public String getPrimaryLanguage()
    {
        return languages.iterator().next();
    }

    public Set<String> getLanguages()
    {
        return languages;
    }

    public String getTitle()
    {
        return title;
    }

    public String getContent()
    {
        return content.toString();
    }

    public void appendMessage(NotificationMessage message)
    {
        if (content.length() > 0) {
            content.append("\n\n");
        }

        String language = message.getPrimaryLanguage();
        String languageString = LANGUAGE_STRING.get(language);
        appendLine(languageString);

        languages.addAll(message.getLanguages());
        if (title == null) {
            title = message.getTitle();
        }
        appendLine(message.getTitle());
        if (userSettingsUrl != null) {
            String userSettingsString = USER_SETTINGS_STRING.get(language);
            appendLine("(" + userSettingsString + userSettingsUrl + ")");
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

    public void appendChildMessage(NotificationMessage message)
    {
        if (content.length() > 0) {
            content.append("\n\n");
        }
        String indent = "  ";
        StringBuilder childContent = new StringBuilder();
        childContent.append(indent);
        childContent.append(message.getTitle());
        childContent.append("\n");
        childContent.append(message.getTitle().replaceAll(".", "-"));
        childContent.append("\n");
        childContent.append(message.getContent());
        content.append(childContent.toString().replaceAll("\n", "\n" + indent));
    }
}
