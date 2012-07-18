package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.PersistentObject;
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
     * Maximum number of enumerated date/times. If {@link #enumerate} exceeds that number
     * an exception is thrown.
     */
    public final int MAX_ENUMERATED_COUNT = PeriodicDateTimeSpecification.MAX_ENUMERATED_COUNT;

    /**
     * Maximum number of enumerated date/times to display by {@link #toString()}.
     */
    public static final int MAX_PRINT_COUNT = PeriodicDateTimeSpecification.MAX_PRINT_COUNT;

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
        if (dateTime == null) {
            throw new IllegalArgumentException("Date/time must not be null!");
        }
        if (duration == null) {
            throw new IllegalArgumentException("Duration must not be null!");
        }
        this.start = dateTime;
        this.duration = duration;
    }

    /**
     * @return {@link #start}
     */
    @OneToOne(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public DateTimeSpecification getStart()
    {
        return start;
    }

    /**
     * @param start sets the {@link #start}
     */
    public void setStart(DateTimeSpecification start)
    {
        this.start = start;
    }

    /**
     * @return {@link #duration}
     */
    @Column
    @Type(type = "Period")
    @Access(AccessType.FIELD)
    public Period getDuration()
    {
        return duration;
    }

    /**
     * @param duration sets the {@link #duration}
     */
    public void setDuration(Period duration)
    {
        this.duration = duration;
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
     * Enumerate list of time slots from single time slot. Time slot can contain for instance
     * periodic date, that can represents multiple absolute date/times.
     * Return only time slots that take place (whole or intersects) inside interval.
     *
     * @param interval
     * @return array of time slots with absolute date/times
     */
    public List<Interval> enumerate(Interval interval)
    {
        return enumerate(interval.getStart(), interval.getEnd());
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
    public final List<Interval> enumerate(DateTime intervalStart, DateTime intervalEnd, int maxCount)
    {
        Interval interval = null;
        if (intervalStart != null && intervalEnd != null) {
            interval = new Interval(intervalStart, intervalEnd);
        }
        List<Interval> slots = new ArrayList<Interval>();
        if (start instanceof PeriodicDateTimeSpecification) {
            PeriodicDateTimeSpecification periodicDateTime = (PeriodicDateTimeSpecification) start;
            for (DateTime dateTime : periodicDateTime.enumerate(intervalStart, intervalEnd, maxCount - slots.size())) {
                Interval slot = new Interval(dateTime, getDuration());
                if ((intervalStart == null && intervalEnd == null)
                        || (interval != null && slot.overlaps(interval))
                        || (intervalStart != null && intervalEnd == null && slot.getEnd().isAfter(intervalStart))
                        || (intervalStart == null && intervalEnd != null && slot.getStart().isBefore(intervalEnd))) {
                    slots.add(slot);
                }
            }
        }
        else {
            if (this.start instanceof AbsoluteDateTimeSpecification) {
                Interval slot = new Interval(((AbsoluteDateTimeSpecification) this.start).getDateTime(), getDuration());
                if ((intervalStart == null && intervalEnd == null)
                        || (interval != null && slot.overlaps(interval))
                        || (intervalStart != null && intervalEnd == null && slot.getEnd().isAfter(intervalStart))
                        || (intervalStart == null && intervalEnd != null && slot.getStart().isBefore(intervalEnd))) {
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
        return getId().equals(slot.getId());

        // TODO: think up how to do infinite equals
        /*List<Interval> slots1 = enumerate();
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
        return true;*/
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
        int index = 0;
        for (Interval slot : enumerate(MAX_PRINT_COUNT)) {
            StringBuilder builder = new StringBuilder();
            builder.append("(");
            builder.append(slot.getStart().toString());
            builder.append(", ");
            builder.append(new Period(slot.getStart(), slot.getEnd()).toString());
            builder.append(")");
            slots.add(builder.toString());
            index++;
            if (index >= (MAX_PRINT_COUNT - 1)) {
                slots.add("...");
                break;
            }
        }
        addCollectionToMap(map, "enumerated", slots);
    }
}
