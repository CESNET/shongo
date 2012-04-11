package cz.cesnet.shongo.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a time slot.
 *
 * @author Martin Srom
 */
public class DateTimeSlot
{
    /**
     * Start date/time.
     */
    private final DateTime start;

    /**
     * Slot duration.
     */
    private final Period duration;

    /**
     * Evaluated slot to absolute date/time slots.
     */
    private ArrayList<AbsoluteDateTimeSlot> slots = null;

    /**
     * Construct time slot.
     *
     * @param dateTime Time slot date/time, can be absolute or relative date/time
     * @param duration Time slot duration (e.g., two hours)
     */
    public DateTimeSlot(DateTime dateTime, Period duration)
    {
        this.start = dateTime;
        this.duration = duration;
    }

    /**
     * Get date/time of time slot.
     *
     * @return date/time
     */
    public DateTime getStart()
    {
        return start;
    }

    /**
     * Get duration of time slot.
     *
     * @return duration
     */
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
    public final boolean isActive()
    {
        return isActive(AbsoluteDateTime.now());
    }

    /**
     * Checks whether time slot takes place at the given referenceDateTime.
     *
     * @param referenceDateTime Reference date/time in which is activity checked
     * @return true if referenced date/time is inside time slot interval,
     *         false otherwise
     */
    public boolean isActive(AbsoluteDateTime referenceDateTime)
    {
        for ( AbsoluteDateTimeSlot slot : getEvaluatedSlots()) {
            if (slot.getStart().beforeOrEqual(referenceDateTime) && slot.getEnd().afterOrEqual(referenceDateTime)) {
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
    private List<AbsoluteDateTimeSlot> getEvaluatedSlots()
    {
        if ( slots == null ) {
            slots = new ArrayList<AbsoluteDateTimeSlot>();
            if (start instanceof PeriodicDateTime) {
                PeriodicDateTime periodicDateTime = (PeriodicDateTime) start;
                for (AbsoluteDateTime dateTime : periodicDateTime.enumerate()) {
                    slots.add(new AbsoluteDateTimeSlot(dateTime, getDuration()));
                }
            }
            else {
                AbsoluteDateTime dateTime = null;
                if ( this.start instanceof AbsoluteDateTime) {
                    dateTime = (AbsoluteDateTime)this.start;
                } else {
                    assert (false) : "Date/time slot can contains only periodic or absolute date/time.";
                }
                if (dateTime != null) {
                    slots.add(new AbsoluteDateTimeSlot(dateTime, getDuration()));
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
    public List<AbsoluteDateTimeSlot> enumerate()
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
    public List<AbsoluteDateTimeSlot> enumerate(AbsoluteDateTime from, AbsoluteDateTime to)
    {
        ArrayList<AbsoluteDateTimeSlot> slots = new ArrayList<AbsoluteDateTimeSlot>();
        for ( AbsoluteDateTimeSlot slot : getEvaluatedSlots()) {
            if ((from == null || slot.getStart().afterOrEqual(from))
                    && (to == null || slot.getEnd().beforeOrEqual(to))) {
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
    final public DateTimeSlot getEarliest()
    {
        return getEarliest(AbsoluteDateTime.now());
    }

    /**
     * Get the earliest time slot since a given date/time.
     *
     * @param referenceDateTime the datetime since which to find the earliest occurrence
     * @return a time slot with absolute date/time
     */
    public AbsoluteDateTimeSlot getEarliest(AbsoluteDateTime referenceDateTime)
    {
        AbsoluteDateTime dateTime = this.start.getEarliest(referenceDateTime);
        if (dateTime == null) {
            return null;
        }
        return new AbsoluteDateTimeSlot(dateTime, getDuration());
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
        List<AbsoluteDateTimeSlot> slots1 = enumerate();
        List<AbsoluteDateTimeSlot> slots2 = slot.enumerate();
        if (slots1.size() != slots2.size()) {
            return false;
        }
        for (int index = 0; index < slots1.size(); index++) {
            DateTimeSlot slot1 = slots1.get(index);
            DateTimeSlot slot2 = slots2.get(index);
            if (slot1.start.equals(slot2.start) == false) {
                return false;
            }
            if (slot1.duration.equals(slot2.duration) == false) {
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
    public String toString()
    {
        List<AbsoluteDateTimeSlot> slots = enumerate();
        StringBuilder result = new StringBuilder();
        for (DateTimeSlot slot : slots) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append("(");
            result.append(slot.start.toString());
            result.append(",");
            result.append(slot.duration.toString());
            result.append(")");
        }
        return "DateTimeSlot [" + result.toString() + "]";
    }
}
