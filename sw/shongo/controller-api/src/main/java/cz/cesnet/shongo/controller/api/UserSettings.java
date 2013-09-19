package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.Converter;
import cz.cesnet.shongo.api.DataMap;
import org.joda.time.DateTimeZone;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a user settings.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserSettings extends AbstractComplexType
{
    public static final Locale LOCALE_ENGLISH = new Locale("en");
    public static final Locale LOCALE_CZECH = new Locale("cs");

    /**
     * User preferred language (e.g., "cs" or "en")
     */
    private Locale locale;

    /**
     * User time zone (e.g., "-08:00")
     */
    private DateTimeZone timeZone;

    /**
     * {@link Boolean#TRUE} or {@link Boolean#FALSE} specifies whether user should act in administrator role
     * (for active session). {@code null} means that user isn't administrator and thus he cannot act as administrator.
     */
    private Boolean adminMode;

    /**
     * Other custom user settings attributes which should be globally stored.
     */
    private Map<String, String> attributes = new HashMap<String, String>();

    /**
     * @return {@link #locale}
     */
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
     * @return {@link #adminMode}
     */
    public Boolean getAdminMode()
    {
        return adminMode;
    }

    /**
     * @param adminMode {@link #adminMode}
     */
    public void setAdminMode(Boolean adminMode)
    {
        this.adminMode = adminMode;
    }

    /**
     * @return {@link #attributes}
     */
    public Map<String, String> getAttributes()
    {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * @param name
     * @return value of attribute with given {@code name}
     */
    public String getAttribute(String name)
    {
        return attributes.get(name);
    }

    /**
     * @param name
     * @return value of attribute with given {@code name}
     */
    public <T extends Enum<T>> T getAttribute(String name, Class<T> enumClass)
    {
        String value = getAttribute(name);
        if (value == null) {
            return null;
        }
        return Converter.convertStringToEnum(value, enumClass);
    }

    /**
     * Add new attribute to {@link #attributes}.
     *
     * @param name of the new attribute
     * @param value of the new attribute
     */
    public void setAttribute(String name, String value)
    {
        if (value == null) {
            attributes.remove(name);
        }
        else {
            attributes.put(name, value);
        }
    }

    @Override
    public String toString()
    {
        return String.format("UserSettings (%s, %s, %s)", locale, timeZone, adminMode);
    }

    private static final String LOCALE = "locale";
    private static final String TIME_ZONE = "timeZone";
    private static final String ADMIN_MODE = "adminMode";
    private static final String ATTRIBUTES = "attributes";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(LOCALE, locale);
        dataMap.set(TIME_ZONE, timeZone);
        dataMap.set(ADMIN_MODE, adminMode);
        dataMap.set(ATTRIBUTES, attributes);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        locale = dataMap.getLocale(LOCALE);
        timeZone = dataMap.getDateTimeZone(TIME_ZONE);
        adminMode = dataMap.getBoolean(ADMIN_MODE);
        attributes = dataMap.getMap(ATTRIBUTES, String.class, String.class);
    }
}
