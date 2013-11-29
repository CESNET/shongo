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
     * Specifies whether main attributes ({@link #locale}) should be loaded from user web service.
     */
    private boolean useWebService;

    /**
     * User preferred language (e.g., "cs" or "en")
     */
    private Locale locale;

    /**
     * User home time zone (e.g., "-08:00")
     */
    private DateTimeZone homeTimeZone;

    /**
     * User current time zone (e.g., "-05:00" when travelling)
     */
    private DateTimeZone currentTimeZone;

    /**
     * {@link Boolean#TRUE} or {@link Boolean#FALSE} specifies whether user should act in administrator role
     * (for active session).
     * Value {@code null} means that user isn't administrator and thus he cannot act as administrator.
     */
    private Boolean adminMode;

    /**
     * Other custom user settings attributes which should be globally stored.
     */
    private Map<String, String> attributes = new HashMap<String, String>();

    /**
     * @return {@link #useWebService}
     */
    public boolean isUseWebService()
    {
        return useWebService;
    }

    /**
     * @param useWebService sets the {@link #useWebService}
     */
    public void setUseWebService(boolean useWebService)
    {
        this.useWebService = useWebService;
    }

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
     * @return {@link #homeTimeZone}
     */
    public DateTimeZone getHomeTimeZone()
    {
        return homeTimeZone;
    }

    /**
     * @param homeTimeZone sets the {@link #homeTimeZone}
     */
    public void setHomeTimeZone(DateTimeZone homeTimeZone)
    {
        this.homeTimeZone = homeTimeZone;
    }

    /**
     * @return {@link #currentTimeZone}
     */
    public DateTimeZone getCurrentTimeZone()
    {
        return currentTimeZone;
    }

    /**
     * @param currentTimeZone sets the {@link #currentTimeZone}
     */
    public void setCurrentTimeZone(DateTimeZone currentTimeZone)
    {
        this.currentTimeZone = currentTimeZone;
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
    public boolean getAttributeBool(String name)
    {
        String value = attributes.get(name);
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value);
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
     * @param name  of the new attribute
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
        return String.format("UserSettings (%s, %s, %s)", locale, homeTimeZone, adminMode);
    }

    private static final String USE_WEB_SERVICE = "useWebService";
    private static final String LOCALE = "locale";
    private static final String HOME_TIME_ZONE = "homeTimeZone";
    private static final String CURRENT_TIME_ZONE = "currentTimeZone";
    private static final String ADMIN_MODE = "adminMode";
    private static final String ATTRIBUTES = "attributes";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USE_WEB_SERVICE, useWebService);
        dataMap.set(LOCALE, locale);
        dataMap.set(HOME_TIME_ZONE, homeTimeZone);
        dataMap.set(CURRENT_TIME_ZONE, currentTimeZone);
        dataMap.set(ADMIN_MODE, adminMode);
        dataMap.set(ATTRIBUTES, attributes);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        useWebService = dataMap.getBool(USE_WEB_SERVICE);
        locale = dataMap.getLocale(LOCALE);
        homeTimeZone = dataMap.getDateTimeZone(HOME_TIME_ZONE);
        currentTimeZone = dataMap.getDateTimeZone(CURRENT_TIME_ZONE);
        adminMode = dataMap.getBoolean(ADMIN_MODE);
        attributes = dataMap.getMap(ATTRIBUTES, String.class, String.class);
    }
}
