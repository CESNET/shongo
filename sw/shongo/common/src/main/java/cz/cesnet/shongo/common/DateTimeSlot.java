package cz.cesnet.shongo.common;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a time slot.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class DateTimeSlot extends PersistentObject
{
    /**
     * Start date/time.
     */
    private DateTimeSpecification start;

    /**
     * Slot duration.
     */
    private Period duration;

    /**
     * Constructor.
     */
    private DateTimeSlot()
    {
    }

    /**
     * Construct time slot.
     *
     * @param dateTime Time slot date/time, can be absolute or relative date/time
     * @param duration Time slot duration (e.g., two hours)
     */
    public DateTimeSlot(DateTimeSpecification dateTime, Period duration)
    {
        this.start = dateTime;
        this.duration = duration;
    }

    /**
     * Get date/time of time slot.
     *
     * @return date/time
     */
    @OneToOne(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public DateTimeSpecification getStart()
    {
        return start;
    }

    /**
     * Get duration of time slot.
     *
     * @return duration
     */
    @Column
    @Type(type = "Period")
    @Access(AccessType.FIELD)
    public Period getDuration()
    {
        return duration;
    }

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
        return enumerate(null);
    }

    /**
     * Enumerate list of time slots from single time slot. Time slot can contain for instance
     * periodic date, that can represents multiple absolute date/times.
     * Return only time slots that take place (whole or intersects) inside interval.
     *
     * @param from
     * @param to
     * @return array of time slots with absolute date/times
     */
    public final List<Interval> enumerate(DateTime from, DateTime to)
    {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Date/time 'from' and 'to' must not be null!");
        }
        return enumerate(new Interval(from, to));
    }

    /**
     * Enumerate list of time slots from single time slot. Time slot can contain for instance
     * periodic date, that can represents multiple absolute date/times.
     * Return only time slots that take place (whole or intersects) inside interval.
     *
     * @param interval
     * @return array of time slots with absolute date/times
     */
    public List<Interval> enumerate(Interval interval)
    {
        List<Interval> slots = new ArrayList<Interval>();
        if (start instanceof PeriodicDateTimeSpecification) {
            PeriodicDateTimeSpecification periodicDateTime = (PeriodicDateTimeSpecification) start;
            for (DateTime dateTime : periodicDateTime.enumerate(interval)) {
                Interval slot = new Interval(dateTime, getDuration());
                if (interval == null || slot.overlaps(interval)) {
                    slots.add(slot);
                }
            }
        }
        else {
            DateTime dateTime = null;
            if (this.start instanceof AbsoluteDateTimeSpecification) {
                Interval slot = new Interval(((AbsoluteDateTimeSpecification) this.start).getDateTime(), getDuration());
                if (interval == null || slot.overlaps(interval)) {
                    slots.add(slot);
                }
            }
            else {
                throw new IllegalStateException("Date/time slot can contains only periodic or absolute date/time.");
            }
        }
        return slots;
    }

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
    public Interval getEarliest(DateTime referenceDateTime)
    {
        DateTime dateTime = this.start.getEarliest(referenceDateTime);
        if (dateTime == null) {
            return null;
        }
        return new Interval(dateTime, getDuration());
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) {
            return true;
        }
        if (object == null || (object instanceof DateTimeSlot) == false) {
            return false;
        }

        DateTimeSlot slot = (DateTimeSlot) object;
        List<Interval> slots1 = enumerate();
        List<Interval> slots2 = slot.enumerate();
        if (slots1.size() != slots2.size()) {
            return false;
        }
        for (int index = 0; index < slots1.size(); index++) {
            Interval slot1 = slots1.get(index);
            Interval slot2 = slots2.get(index);
            if (slot1.equals(slot2) == false) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = 23;
        result = 37 * result + start.hashCode();
        result = 37 * result + duration.hashCode();
        return result;
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        map.put("start", start.toString());
        map.put("duration", duration.toString());

        List<String> slots = new ArrayList<String>();
        for (Interval slot : enumerate()) {
            StringBuilder builder = new StringBuilder();
            builder.append("(");
            builder.append(slot.getStart().toString());
            builder.append(", ");
            builder.append(new Period(slot.getStart(), slot.getEnd()).toString());
            builder.append(")");
            slots.add(builder.toString());
        }
        addCollectionToMap(map, "enumerated", slots);
    }
}
