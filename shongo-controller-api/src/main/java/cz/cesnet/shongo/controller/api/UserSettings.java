package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.Converter;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.SystemPermission;
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
     * Specifies whether main attributes ({@link #locale} and {@link #homeTimeZone}) should be loaded from user web service.
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
     * Specifies whether user wants to receive system administrator notifications
     * (in case that he is system administrator).
     */
    private boolean systemAdministratorNotifications;

    /**
     * Specifies whether user wants to receive resource administrator notifications
     * (in case that he is resource administrator).
     */
    private boolean resourceAdministratorNotifications;

    /**
     * Specifies whether user should act in administrator role (for active session).
     * Only valid when user have {@link SystemPermission#ADMINISTRATION}.
     */
    private boolean administrationMode;

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
     * @return {@link #systemAdministratorNotifications}
     */
    public boolean isSystemAdministratorNotifications()
    {
        return systemAdministratorNotifications;
    }

    /**
     * @param systemAdministratorNotifications sets the {@link #systemAdministratorNotifications}
     */
    public void setSystemAdministratorNotifications(boolean systemAdministratorNotifications)
    {
        this.systemAdministratorNotifications = systemAdministratorNotifications;
    }

    /**
     * @return {@link #resourceAdministratorNotifications}
     */
    public boolean isResourceAdministratorNotifications()
    {
        return resourceAdministratorNotifications;
    }

    /**
     * @param resourceAdministratorNotifications sets the {@link #resourceAdministratorNotifications}
     */
    public void setResourceAdministratorNotifications(boolean resourceAdministratorNotifications)
    {
        this.resourceAdministratorNotifications = resourceAdministratorNotifications;
    }

    /**
     * @return {@link #administrationMode}
     */
    public boolean getAdministrationMode()
    {
        return administrationMode;
    }

    /**
     * @param administrationMode {@link #administrationMode}
     */
    public void setAdministrationMode(boolean administrationMode)
    {
        this.administrationMode = administrationMode;
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
    public Integer getAttributeInteger(String name)
    {
        String value = attributes.get(name);
        if (value == null) {
            return null;
        }
        return Integer.parseInt(value);
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
            removeAttribute(name);
        }
        else {
            attributes.put(name, value);
        }
    }

    /**
     * @param name to be removed
     */
    public void removeAttribute(String name)
    {
        attributes.remove(name);
    }

    @Override
    public String toString()
    {
        return String.format("UserSettings (%s, %s, %s)", locale, homeTimeZone, administrationMode);
    }

    private static final String USE_WEB_SERVICE = "useWebService";
    private static final String LOCALE = "locale";
    private static final String HOME_TIME_ZONE = "homeTimeZone";
    private static final String CURRENT_TIME_ZONE = "currentTimeZone";
    private static final String SYSTEM_ADMINISTRATOR_NOTIFICATIONS = "systemAdministratorNotifications";
    private static final String RESOURCE_ADMINISTRATOR_NOTIFICATIONS = "resourceAdministratorNotifications";
    private static final String ADMINISTRATION_MODE = "administrationMode";
    private static final String ATTRIBUTES = "attributes";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USE_WEB_SERVICE, useWebService);
        dataMap.set(LOCALE, locale);
        dataMap.set(HOME_TIME_ZONE, homeTimeZone);
        dataMap.set(CURRENT_TIME_ZONE, currentTimeZone);
        dataMap.set(SYSTEM_ADMINISTRATOR_NOTIFICATIONS, systemAdministratorNotifications);
        dataMap.set(RESOURCE_ADMINISTRATOR_NOTIFICATIONS, resourceAdministratorNotifications);
        dataMap.set(ADMINISTRATION_MODE, administrationMode);
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
        systemAdministratorNotifications = dataMap.getBool(SYSTEM_ADMINISTRATOR_NOTIFICATIONS);
        resourceAdministratorNotifications = dataMap.getBool(RESOURCE_ADMINISTRATOR_NOTIFICATIONS);
        administrationMode = dataMap.getBool(ADMINISTRATION_MODE);
        attributes = dataMap.getMap(ATTRIBUTES, String.class, String.class);
    }
}
