package cz.cesnet.shongo.controller.settings;

import cz.cesnet.shongo.PersistentObject;
import org.hibernate.annotations.Type;
import org.joda.time.DateTimeZone;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.Locale;

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
     * Preferred locale.
     */
    private Locale locale;

    /**
     * {@link DateTimeZone} of the user.
     */
    private DateTimeZone timeZone;

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
}
