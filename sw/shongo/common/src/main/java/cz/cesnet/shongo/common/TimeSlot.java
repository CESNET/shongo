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
     * Enumarete all time slots
     *
     * @return
     */
    public TimeSlot<AbsoluteDateTime>[] enumerate()
    {
        ArrayList<TimeSlot<AbsoluteDateTime>> slots = new ArrayList<TimeSlot<AbsoluteDateTime>>();
        if ( getDateTime() instanceof PeriodicDateTime ) {
            PeriodicDateTime periodicDateTime = (PeriodicDateTime)getDateTime();
            for ( AbsoluteDateTime dateTime : periodicDateTime.enumerate() ) {
                slots.add(new TimeSlot<AbsoluteDateTime>(dateTime, getDuration()));
            }
        }
        return slots.toArray(new TimeSlot[]{});
    }

}
