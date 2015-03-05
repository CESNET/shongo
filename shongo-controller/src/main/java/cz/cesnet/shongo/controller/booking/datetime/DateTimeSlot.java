package cz.cesnet.shongo.controller.booking.datetime;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;
import java.util.List;

/**
 * Represents a base class for all possible types of date/time slots.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class DateTimeSlot extends SimplePersistentObject implements Cloneable
{
    /**
     * Maximum number of enumerated date/times. If {@link #enumerate} exceeds that number
     * an exception is thrown.
     */
    public final int MAX_ENUMERATED_COUNT = PeriodicDateTime.MAX_ENUMERATED_COUNT;

    /**
     * @return duration of the {@link DateTimeSlot}.
     */
    @Transient
    public abstract Period getDuration();

    /**
     * Checks whether time slot takes place at the moment.
     *
     * @return true if time slot is taking place now,
     *         false otherwise
     */
    @Transient
    public final boolean isActive()
    {
        return isActive(DateTime.now());
    }

    /**
     * Checks whether time slot takes place at the given referenceDateTime.
     *
     * @param referenceDateTime Reference date/time in which is activity checked
     * @return true if referenced date/time is inside time slot interval,
     *         false otherwise
     */
    @Transient
    public boolean isActive(DateTime referenceDateTime)
    {
        Period duration = getDuration();
        for (Interval slot : enumerate(referenceDateTime.minus(duration), referenceDateTime.plus(duration))) {
            if (slot.contains(referenceDateTime)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enumerate list of time slots from single time slot. Time slot can contain for instance
     * periodic date, that can represents multiple absolute date/times.
     *
     * @return array of time slots with absolute date/times
     */
    public final List<Interval> enumerate()
    {
        return enumerate(null, null);
    }

    /**
     * Enumerate list of time slots from single time slot. Time slot can contain for instance
     * periodic date, that can represents multiple absolute date/times.
     *
     * @param maxCount
     * @return array of time slots with absolute date/times
     */
    public final List<Interval> enumerate(int maxCount)
    {
        return enumerate(null, null, maxCount);
    }

    /**
     * Enumerate collection of date/time slots from this {@link DateTimeSlot} (it can contain, e.g.,
     * periodic date, that can represents multiple absolute date/times). Return only date/time slots
     * that take place (whole or intersects) inside interval.
     *
     * @param interval which must all returned date/time slots intersect
     * @return list of all absolute date/time slots which intersect given {@code interval}
     */
    public List<Interval> enumerate(Interval interval)
    {
        return enumerate(interval.getStart(), interval.getEnd());
    }

    /**
     * Enumerate collection of date/time slots from this {@link DateTimeSlot} (it can contain, e.g.,
     * periodic date, that can represents multiple absolute date/times). Return only date/time slots
     * that take place (whole or intersects) inside interval.
     *
     * @param intervalStart
     * @param intervalEnd
     * @return list of all absolute date/time slots which intersect interval constructed from
     *         given {@code intervalStart} and {@code intervalEnd}
     */
    public final List<Interval> enumerate(DateTime intervalStart, DateTime intervalEnd)
    {
        List<Interval> slotList = enumerate(intervalStart, intervalEnd, MAX_ENUMERATED_COUNT);
        if (slotList.size() >= MAX_ENUMERATED_COUNT) {
            throw new IllegalArgumentException("Cannot enumerate slots for interval '"
                    + (intervalStart != null ? intervalStart
                    .toString() : "null") + "'-'" + (intervalEnd != null ? intervalEnd.toString() : "null")
                    + "' because maximum number " + MAX_ENUMERATED_COUNT + " was reached!");
        }
        return slotList;
    }

    /**
     * Enumerate list of time slots from single time slot. Time slot can contain for instance
     * periodic date, that can represents multiple absolute date/times.
     * Return only time slots that take place (whole or intersects) inside interval.
     *
     * @param intervalStart
     * @param intervalEnd
     * @return array of time slots with absolute date/times
     */
    protected abstract List<Interval> enumerate(DateTime intervalStart, DateTime intervalEnd, int maxCount);

    /**
     * Get the earliest time slot from now.
     *
     * @return a time slot with absolute date/time
     */
    @Transient
    final public Interval getEarliest()
    {
        return getEarliest(DateTime.now());
    }

    /**
     * Get the earliest time slot since a given date/time.
     *
     * @param referenceDateTime the datetime since which to find the earliest occurrence
     * @return a time slot with absolute date/time
     */
    @Transient
    public abstract Interval getEarliest(DateTime referenceDateTime);

    /**
     * Will the datetime take place in future (strict inequality)?
     *
     * @return boolean
     */
    public final boolean willOccur()
    {
        Interval slot = getEarliest();
        return slot != null && slot.isAfter(DateTime.now());
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

    /**
     * Checks if given slots is exactly within this.
     * @param slot slot to compare
     * @return true if the given slot is within this, else otherwise
     */
    public abstract boolean contains(Interval slot);

    /**
     * @return converted {@link DateTimeSlot} to API
     */
    public abstract Object toApi();

    /**
     * @param slotApi
     * @return {@link DateTimeSlot} from given {@code slotApi}
     */
    public static DateTimeSlot createFromApi(Object slotApi)
    {
        DateTimeSlot dateTimeSlot;
        if (slotApi instanceof Interval) {
            dateTimeSlot = new AbsoluteDateTimeSlot();
        }
        else if (slotApi instanceof cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot) {
            dateTimeSlot = new PeriodicDateTimeSlot();
        }
        else {
            throw new TodoImplementException(slotApi.getClass());
        }
        dateTimeSlot.fromApi(slotApi);
        return dateTimeSlot;
    }

    /**
     * Synchronize from given {@code slotApi}.
     *
     * @param slotApi to synchronize from
     */
    public abstract void fromApi(Object slotApi);

    /**
     * @param slotApi
     * @return true whether this {@link DateTimeSlot} is created from given {@code slotApi}
     *         false otherwise
     */
    public abstract boolean equalsApi(Object slotApi);

    @Override
    public DateTimeSlot clone() throws CloneNotSupportedException
    {
        DateTimeSlot dateTimeSlot = (DateTimeSlot) super.clone();
        dateTimeSlot.setIdNull();
        return dateTimeSlot;
    }
}
