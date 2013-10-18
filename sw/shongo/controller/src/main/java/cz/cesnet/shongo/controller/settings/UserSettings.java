package cz.cesnet.shongo.controller.settings;

import cz.cesnet.shongo.SimplePersistentObject;
import org.hibernate.annotations.Type;
import org.joda.time.DateTimeZone;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represent a global user settings.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class UserSettings extends SimplePersistentObject
{
    /**
     * User-id.
     */
    private String userId;

    /**
     * Preferred locale.
     */
    private Locale locale;

    /**
     * {@link DateTimeZone} of the user.
     */
    private DateTimeZone timeZone;

    /**
     * Other custom user settings attributes which should be globally stored.
     */
    private Map<String, String> attributes = new HashMap<String, String>();

    /**
     * @return {@link #userId}
     */
    @Column(nullable = false, unique = true)
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    /**
     * @return {@link #locale}
     */
    @Column
    @Type(type = "Locale")
    @Access(AccessType.FIELD)
    public Locale getLocale()
    {
        return locale;
    }

    /**
     * @param locale sets the {@link #locale}
     */
    public void setLocale(Locale locale)
    {
        this.locale = locale;
    }

    /**
     * @return {@link #timeZone}
     */
    @Column
    @Type(type = "DateTimeZone")
    @Access(AccessType.FIELD)
    public DateTimeZone getTimeZone()
    {
        return timeZone;
    }

    /**
     * @param timeZone sets the {@link #timeZone}
     */
    public void setTimeZone(DateTimeZone timeZone)
    {
        this.timeZone = timeZone;
    }

    /**
     * @return {@link #attributes}
     */
    @ElementCollection
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    @Access(AccessType.FIELD)
    public Map<String, String> getAttributes()
    {
        return attributes;
    }

    /**
     * Load this {@link UserSettings} from given {@code userSettingsApi}.
     *
     * @param userSettingsApi
     */
    public void fromApi(cz.cesnet.shongo.controller.api.UserSettings userSettingsApi)
    {
        setLocale(userSettingsApi.getLocale());
        setTimeZone(userSettingsApi.getTimeZone());
        attributes.clear();
        for (Map.Entry<String, String> attribute : userSettingsApi.getAttributes().entrySet()) {
            String attributeName = attribute.getKey();
            String attributeValue = attribute.getValue();
            if (attributeName != null && attributeValue != null) {
                attributes.put(attributeName, attributeValue);
            }
        }
    }

    /**
     * Store this {@link UserSettings} to given {@code userSettingsApi}.
     *
     * @param userSettingsApi
     */
    public void toApi(cz.cesnet.shongo.controller.api.UserSettings userSettingsApi)
    {
        userSettingsApi.setLocale(getLocale());
        userSettingsApi.setTimeZone(getTimeZone());
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            userSettingsApi.setAttribute(attribute.getKey(), attribute.getValue());
        }
    }
}
