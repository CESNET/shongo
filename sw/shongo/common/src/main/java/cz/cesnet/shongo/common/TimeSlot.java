package cz.cesnet.shongo.common;

import java.util.ArrayList;

/**
 * Represents a time slot
 *
 * @author Martin Srom
 */
public class TimeSlot<T extends DateTime>
{
    private T dateTime;
    private Period duration;

    /**
     * Constructor
     *
     * @param dateTime
     * @param duration
     */
    public TimeSlot(T dateTime, Period duration)
    {
        this.dateTime = dateTime;
        this.duration = duration;
    }

    /**
     * Get date/time of time slot
     *
     * @return date/time
     */
    public DateTime getDateTime()
    {
        return dateTime;
    }

    /**
     * Get duration of time slot
     *
     * @return duration
     */
    public Period getDuration()
    {
        return duration;
    }

    /**
     * Is taking place at the moment
     *
     * @return boolean
     */
    public boolean isActive()
    {
        throw new RuntimeException("TODO: Implement TimeSlot.isActive");
    }

    /**
     * Get the earliest time slot.
     *
     * @return time slot with absolute date/time
     */
    public TimeSlot<AbsoluteDateTime> getEarliest()
    {
        return new TimeSlot<AbsoluteDateTime>(dateTime.getEarliest(), getDuration());
    }

    /**
     * Enumerate list of time slots from single time slot. Time slot can contain for instance
     * periodic date, that can represents multiple absolute date/times.
     *
     * @return array of time slots with absolute date/times
     */
    public TimeSlot<AbsoluteDateTime>[] enumerate()
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
    public TimeSlot<AbsoluteDateTime>[] enumerate(AbsoluteDateTime from, AbsoluteDateTime to)
    {
        ArrayList<TimeSlot<AbsoluteDateTime>> slots = new ArrayList<TimeSlot<AbsoluteDateTime>>();
        if (getDateTime() instanceof PeriodicDateTime) {
            PeriodicDateTime periodicDateTime = (PeriodicDateTime) getDateTime();
            for (AbsoluteDateTime dateTime : periodicDateTime.enumerate(from, to)) {
                slots.add(new TimeSlot<AbsoluteDateTime>(dateTime, getDuration()));
            }
        }
        else {
            AbsoluteDateTime dateTime = getDateTime().getEarliest();
            if ((from == null || dateTime.after(from) || dateTime.equals(from))
                    && (to == null || dateTime.before(to) || dateTime.equals(to))) {
                slots.add(new TimeSlot<AbsoluteDateTime>(dateTime, getDuration()));
            }
        }
        return slots.toArray(new TimeSlot[]{});
    }

}
