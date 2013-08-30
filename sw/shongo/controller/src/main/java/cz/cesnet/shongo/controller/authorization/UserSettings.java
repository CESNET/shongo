package cz.cesnet.shongo.controller.authorization;

import cz.cesnet.shongo.PersistentObject;
import org.hibernate.annotations.Type;
import org.joda.time.DateTimeZone;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Represent a global user settings.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class UserSettings extends PersistentObject
{
    /**
     * User-id.
     */
    private String userId;

    /**
     * Preferred language.
     */
    private String language;

    /**
     * {@link DateTimeZone} of the user.
     */
    private DateTimeZone dateTimeZone;

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
     * @return {@link #language}
     */
    @Column
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
    @Column
    @Type(type = "DateTimeZone")
    @Access(AccessType.FIELD)
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
}
