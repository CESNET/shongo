package cz.cesnet.shongo.common;

import javax.persistence.*;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Represents an arbitrary Date/Time.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class DateTime extends PersistentObject
{
    /**
     * Get the earliest Date/Time since a given datetime (strict inequality).
     *
     * @param referenceDateTime the datetime since which to find the earliest occurrence
     * @return absolute Date/Time, or <code>null</code> if the datetime won't take place since referenceDateTime
     */
    @Transient
    public abstract AbsoluteDateTime getEarliest(AbsoluteDateTime referenceDateTime);

    /**
     * Get the earliest Date/Time from now.
     *
     * @return absolute Date/Time, or <code>null</code> if the datetime won't take place
     */
    @Transient
    public final AbsoluteDateTime getEarliest()
    {
        return getEarliest(now());
    }

    /**
     * Will the datetime take place in future (strict inequality)?
     *
     * @return boolean
     */
    public final boolean willOccur()
    {
        AbsoluteDateTime dateTime = getEarliest();
        return dateTime.after(now());
    }

    /**
     * Checks whether this datetime will take place since a given absolute datetime (strict inequality).
     *
     * @param referenceDateTime the datetime take as "now" for evaluating future
     * @return true if this datetime will take place at least once after or in referenceDateTime,
     *         false if not
     */
    public final boolean willOccur(AbsoluteDateTime referenceDateTime)
    {
        return getEarliest(referenceDateTime) != null;
    }

    /**
     * Get current Date/Time
     *
     * @return current Date/Time
     */
    public static final AbsoluteDateTime now()
    {
        Calendar now = Calendar.getInstance();
        now.setTimeZone(TimeZone.getTimeZone("UTC"));

        Date date = new Date(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        Time time = new Time(now.get(Calendar.HOUR), now.get(Calendar.MINUTE), now.get(Calendar.SECOND));
        return new AbsoluteDateTime(date, time);
    }
}
