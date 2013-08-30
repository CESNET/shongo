package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.DataMap;
import org.joda.time.DateTimeZone;

/**
 * Represents a user settings.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UserSettings extends AbstractComplexType
{
    /**
     * User preferred language (e.g., "cs" or "en")
     */
    private String language;

    /**
     * User time zone (e.g., "-08:00")
     */
    private DateTimeZone dateTimeZone;

    /**
     * Specifies whether user should act in administrator role (for active session). {@code null} means that user isn't
     * administrator.
     */
    private Boolean adminMode;

    /**
     * @return {@link #language}
     */
    public String getLanguage()
    {
        return language;
    }

    /**
     * @param language sets the {@link #language}
     */
    public void setLanguage(String language)
    {
        this.language = language;
    }

    /**
     * @return {@link #dateTimeZone}
     */
    public DateTimeZone getDateTimeZone()
    {
        return dateTimeZone;
    }

    /**
     * @param dateTimeZone sets the {@link #dateTimeZone}
     */
    public void setDateTimeZone(DateTimeZone dateTimeZone)
    {
        this.dateTimeZone = dateTimeZone;
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

    @Override
    public String toString()
    {
        return String.format("UserSettings (%s, %s, %s)", language, dateTimeZone, adminMode);
    }

    private static final String LANGUAGE = "language";
    private static final String DATE_TIME_ZONE = "dateTimeZone";
    private static final String ADMIN_MODE = "adminMode";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(LANGUAGE, language);
        dataMap.set(DATE_TIME_ZONE, dateTimeZone);
        dataMap.set(ADMIN_MODE, adminMode);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        language = dataMap.getString(LANGUAGE);
        dateTimeZone = dataMap.getDateTimeZone(DATE_TIME_ZONE);
        adminMode = dataMap.getBoolean(ADMIN_MODE);
    }
}
