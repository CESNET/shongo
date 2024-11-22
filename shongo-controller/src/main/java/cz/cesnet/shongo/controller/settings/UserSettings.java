package cz.cesnet.shongo.controller.settings;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.controller.api.Controller;
import cz.cesnet.shongo.hibernate.PersistentDateTimeZone;
import cz.cesnet.shongo.hibernate.PersistentLocale;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.usertype.UserTypeLegacyBridge;
import org.joda.time.DateTimeZone;

import jakarta.persistence.*;
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
     * Specifies whether main attributes ({@link #locale}) should be loaded from user web service.
     */
    private boolean useWebService;

    /**
     * Preferred locale.
     */
    private Locale locale;

    /**
     * Home {@link DateTimeZone} of the user.
     */
    private DateTimeZone homeTimeZone;

    /**
     * Current {@link DateTimeZone} of the user (e.g., when travelling).
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
     * Other custom user settings attributes which should be globally stored.
     */
    private Map<String, String> attributes = new HashMap<String, String>();

    /**
     * @return {@link #userId}
     */
    @Column(nullable = false, unique = true, length = Controller.USER_ID_COLUMN_LENGTH)
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
     * @return {@link #useWebService}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
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
    @Column(length = PersistentLocale.LENGTH)
    @Type(PersistentLocale.class)
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
     * @return {@link #homeTimeZone}
     */
    @Column(length = PersistentDateTimeZone.LENGTH)
    @Type(PersistentDateTimeZone.class)
    @Access(AccessType.FIELD)
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
    @Column(length = PersistentDateTimeZone.LENGTH)
    @Type(PersistentDateTimeZone.class)
    @Access(AccessType.FIELD)
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
    @Column(nullable = false, columnDefinition = "boolean default true")
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
    @Column(nullable = false, columnDefinition = "boolean default true")
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
        setUseWebService(userSettingsApi.isUseWebService());
        setLocale(userSettingsApi.getLocale());
        setHomeTimeZone(userSettingsApi.getHomeTimeZone());
        setCurrentTimeZone(userSettingsApi.getCurrentTimeZone());
        setSystemAdministratorNotifications(userSettingsApi.isSystemAdministratorNotifications());
        setResourceAdministratorNotifications(userSettingsApi.isResourceAdministratorNotifications());
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
        userSettingsApi.setUseWebService(isUseWebService());
        userSettingsApi.setLocale(getLocale());
        userSettingsApi.setHomeTimeZone(getHomeTimeZone());
        userSettingsApi.setCurrentTimeZone(getCurrentTimeZone());
        userSettingsApi.setResourceAdministratorNotifications(isResourceAdministratorNotifications());
        userSettingsApi.setSystemAdministratorNotifications(isSystemAdministratorNotifications());
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            userSettingsApi.setAttribute(attribute.getKey(), attribute.getValue());
        }
    }
}
