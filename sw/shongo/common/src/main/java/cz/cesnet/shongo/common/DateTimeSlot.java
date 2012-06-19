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
    private final DateTimeSpecification start;

    /**
     * Slot duration.
     */
    private final Period duration;

    /**
     * Evaluated slot to absolute date/time slots.
     */
    private List<Interval> slots = null;

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
        for (Interval slot : getEvaluatedSlots()) {
            if (slot.contains(referenceDateTime)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get list of absolute date/time slots from this slot.
     *
     * @return list of absolute date/time slots
     */
    @Transient
    private List<Interval> getEvaluatedSlots()
    {
        if (slots == null) {
            slots = new ArrayList<Interval>();
            if (start instanceof PeriodicDateTimeSpecification) {
                PeriodicDateTimeSpecification periodicDateTime = (PeriodicDateTimeSpecification) start;
                for (DateTime dateTime : periodicDateTime.enumerate()) {
                    slots.add(new Interval(dateTime, getDuration()));
                }
            }
            else {
                DateTime dateTime = null;
                if (this.start instanceof AbsoluteDateTimeSpecification) {
                    slots.add(new Interval(((AbsoluteDateTimeSpecification) this.start).getDateTime(), getDuration()));
                }
                else {
                    throw new IllegalStateException("Date/time slot can contains only periodic or absolute date/time.");
                }
            }
        }
        return slots;
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
     * Return only time slots that take place inside interval defined by from - to.
     *
     * @return array of time slots with absolute date/times
     */
    public List<Interval> enumerate(DateTime from, DateTime to)
    {
        if (from != null || to != null) {
            // TODO: now DateTimeSlot.getEvaluatedSlots() can never end
            throw new RuntimeException("TODO: Implement DateTimeSlot.getEvaluatedSlots with respect to interval!");
        }
        ArrayList<Interval> slots = new ArrayList<Interval>();
        for (Interval slot : getEvaluatedSlots()) {
            if ((from == null || !slot.getStart().isBefore(from))
                    && (to == null || !slot.getEnd().isAfter(to))) {
                slots.add(slot);
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
