package cz.cesnet.shongo.common;

import java.util.ArrayList;

/**
 * Represents a time slot.
 *
 * @author Martin Srom
 */
public class TimeSlot
{
    private final DateTime dateTime;
    private final Period duration;

    /**
     * Construct time slot.
     *
     * @param dateTime Time slot date/time, can be absolute or relative date/time
     * @param duration Time slot duration (e.g., two hours)
     */
    public TimeSlot(DateTime dateTime, Period duration)
    {
        this.dateTime = dateTime;
        this.duration = duration;
    }

    /**
     * Get date/time of time slot.
     *
     * @return date/time
     */
    public DateTime getDateTime()
    {
        return dateTime;
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
        throw new RuntimeException("TODO: Implement TimeSlot.isActive");
    }

    /**
     * Enumerate list of time slots from single time slot. Time slot can contain for instance
     * periodic date, that can represents multiple absolute date/times.
     *
     * @return array of time slots with absolute date/times
     */
    public TimeSlot[] enumerate()
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
    public TimeSlot[] enumerate(AbsoluteDateTime from, AbsoluteDateTime to)
    {
        ArrayList<TimeSlot> slots = new ArrayList<TimeSlot>();
        if (dateTime instanceof PeriodicDateTime) {
            PeriodicDateTime periodicDateTime = (PeriodicDateTime) dateTime;
            for (AbsoluteDateTime dateTime : periodicDateTime.enumerate(from, to)) {
                slots.add(new TimeSlot(dateTime, getDuration()));
            }
        }
        else {
            AbsoluteDateTime dateTime = null;
            if ( this.dateTime instanceof AbsoluteDateTime) {
                dateTime = (AbsoluteDateTime)this.dateTime;
            } else {
                dateTime = getDateTime().getEarliest();
            }
            if (dateTime != null) {
                if ((from == null || dateTime.after(from) || dateTime.equals(from))
                        && (to == null || dateTime.before(to) || dateTime.equals(to))) {
                    slots.add(new TimeSlot(dateTime, getDuration()));
                }
            }
        }
        return slots.toArray(new TimeSlot[slots.size()]);
    }

    /**
     * Get the earliest time slot from now.
     *
     * @return a time slot with absolute date/time
     */
    final public TimeSlot getEarliest()
    {
        return getEarliest(AbsoluteDateTime.now());
    }

    /**
     * Get the earliest time slot since a given date/time.
     *
     * @param referenceDateTime the datetime since which to find the earliest occurrence
     * @return a time slot with absolute date/time
     */
    public TimeSlot getEarliest(AbsoluteDateTime referenceDateTime)
    {
        AbsoluteDateTime dateTime = this.dateTime.getEarliest(referenceDateTime);
        if (dateTime == null) {
            return null;
        }
        return new TimeSlot(dateTime, getDuration());
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        TimeSlot timeSlot = (TimeSlot) object;
        TimeSlot[] timeSlots1 = enumerate();
        TimeSlot[] timeSlots2 = timeSlot.enumerate();
        if (timeSlots1.length != timeSlots2.length) {
            return false;
        }
        for (int index = 0; index < timeSlots1.length; index++) {
            if (timeSlots1[index].dateTime.equals(timeSlots2[index].dateTime) == false) {
                return false;
            }
            if (timeSlots1[index].duration.equals(timeSlots2[index].duration) == false) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = 23;
        result = 37 * result + dateTime.hashCode();
        result = 37 * result + duration.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        TimeSlot[] dateTimes = enumerate();
        StringBuilder result = new StringBuilder();
        for (TimeSlot timeSlot : dateTimes) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append("(");
            result.append(timeSlot.dateTime.toString());
            result.append(",");
            result.append(timeSlot.duration.toString());
            result.append(")");
        }
        return "TimeSlot [" + result.toString() + "]";
    }
}
