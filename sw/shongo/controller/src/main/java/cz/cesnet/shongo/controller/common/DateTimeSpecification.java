package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.PersistentObject;
import org.joda.time.DateTime;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

/**
 * Represents a specification of Date/Time.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class DateTimeSpecification extends PersistentObject
{
    /**
     * Get the earliest Date/Time from now.
     *
     * @return absolute Date/Time, or <code>null</code> if the datetime won't take place
     */
    @Transient
    public final DateTime getEarliest()
    {
        return getEarliest(DateTime.now());
    }

    /**
     * Get the earliest Date/Time since a given datetime (strict inequality).
     *
     * @param referenceDateTime the datetime since which to find the earliest occurrence
     * @return absolute Date/Time, or <code>null</code> if the datetime won't take place since referenceDateTime
     */
    @Transient
    public abstract DateTime getEarliest(DateTime referenceDateTime);

    /**
     * Will the datetime take place in future (strict inequality)?
     *
     * @return boolean
     */
    public final boolean willOccur()
    {
        DateTime dateTime = getEarliest();
        return dateTime.isAfter(DateTime.now());
    }

    /**
     * Checks whether this datetime will take place since a given absolute datetime (strict inequality).
     *
     * @param referenceDateTime the datetime take as "now" for evaluating future
     * @return true if this datetime will take place at least once after or in referenceDateTime,
     *         false if not
     */
    public final boolean willOccur(DateTime referenceDateTime)
    {
        return getEarliest(referenceDateTime) != null;
    }
}
