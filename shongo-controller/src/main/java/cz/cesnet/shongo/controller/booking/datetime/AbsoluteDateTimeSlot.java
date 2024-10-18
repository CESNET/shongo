package cz.cesnet.shongo.controller.booking.datetime;

import cz.cesnet.shongo.hibernate.PersistentDateTime;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single {@link DateTimeSlot} defined by single absolute interval.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AbsoluteDateTimeSlot extends DateTimeSlot
{
    /**
     * Starting date/time of the slot.
     */
    private DateTime slotStart;

    /**
     * Ending date/time of the slot.
     */
    private DateTime slotEnd;

    /**
     * Constructor.
     */
    public AbsoluteDateTimeSlot()
    {
    }

    /**
     * Constructor.
     *
     * @param slotStart sets the {@link #slotStart}
     * @param slotEnd   sets the {@link #slotEnd}
     */
    public AbsoluteDateTimeSlot(DateTime slotStart, DateTime slotEnd)
    {
        setSlotStart(slotStart);
        setSlotEnd(slotEnd);
    }


    /**
     * Constructor.
     *
     * @param slotStart sets the {@link #slotStart}
     * @param slotEnd   sets the {@link #slotEnd}
     */
    public AbsoluteDateTimeSlot(String slotStart, String slotEnd)
    {
        setSlotStart(DateTime.parse(slotStart));
        setSlotEnd(DateTime.parse(slotEnd));
    }

    /**
     * Constructor.
     *
     * @param slotStart sets the {@link #slotStart}
     * @param duration  sets the {@link #slotEnd} as {@code slotStart} plus {@code duration}
     */
    public AbsoluteDateTimeSlot(DateTime slotStart, Period duration)
    {
        setSlotStart(slotStart);
        setSlotEnd(slotStart.plus(duration));
    }

    /**
     * @return {@link #slotStart}
     */
    @Column
    @org.hibernate.annotations.Type(value = PersistentDateTime.class)
    @Access(AccessType.FIELD)
    public DateTime getSlotStart()
    {
        return slotStart;
    }

    /**
     * @param slotStart sets the {@link #slotStart}
     */
    public void setSlotStart(DateTime slotStart)
    {
        this.slotStart = slotStart;
    }

    /**
     * @return {@link #slotEnd}
     */
    @Column
    @org.hibernate.annotations.Type(value = PersistentDateTime.class)
    @Access(AccessType.FIELD)
    public DateTime getSlotEnd()
    {
        return slotEnd;
    }

    /**
     * @param slotEnd sets the {@link #slotEnd}
     */
    public void setSlotEnd(DateTime slotEnd)
    {
        this.slotEnd = slotEnd;
    }

    /**
     * @return absolute date/time slot ({@link #slotStart}, {@link #slotEnd})
     */
    @Transient
    public Interval getSlot()
    {
        return new Interval(slotStart, slotEnd);
    }

    /**
     * @param slot sets the absolute date/time slot
     */
    @Transient
    public void setSlot(Interval slot)
    {
        setSlotStart(slot.getStart());
        setSlotEnd(slot.getEnd());
    }

    @Override
    @Transient
    public Period getDuration()
    {
        return getSlot().toPeriod();
    }

    @Override
    public final List<Interval> enumerate(DateTime intervalStart, DateTime intervalEnd, int maxCount)
    {
        Interval interval = null;
        if (intervalStart != null && intervalEnd != null) {
            interval = new Interval(intervalStart, intervalEnd);
        }
        List<Interval> slots = new ArrayList<Interval>();

        Interval slot = new Interval(slotStart, getDuration());
        if ((intervalStart == null && intervalEnd == null)
                || (interval != null && slot.overlaps(interval))
                || (intervalStart != null && intervalEnd == null && slot.getEnd().isAfter(intervalStart))
                || (intervalStart == null && intervalEnd != null && slot.getStart().isBefore(intervalEnd))) {
            slots.add(slot);
        }
        return slots;
    }

    @Override
    public Interval getEarliest(DateTime referenceDateTime)
    {
        if (referenceDateTime != null) {
            referenceDateTime = referenceDateTime.minus(getDuration());
            if (!slotStart.isAfter(referenceDateTime)) {
                return null;

            }
        }
        return new Interval(slotStart, slotEnd);
    }

    @Override
    public boolean contains(Interval slot) {
        if (this.getSlot().overlaps(slot)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) {
            return true;
        }
        if (object == null || (object instanceof AbsoluteDateTimeSlot) == false) {
            return false;
        }

        AbsoluteDateTimeSlot absoluteDateTimeSlot = (AbsoluteDateTimeSlot) object;
        if (getId() != null) {
            return getId().equals(absoluteDateTimeSlot.getId());
        }
        if (!slotStart.equals(absoluteDateTimeSlot.slotStart)) {
            return false;
        }
        if (!slotEnd.equals(absoluteDateTimeSlot.slotEnd)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = 23;
        result = 37 * result + slotStart.hashCode();
        result = 37 * result + slotEnd.hashCode();
        return result;
    }

    @Override
    public Object toApi()
    {
        return getSlot();
    }

    @Override
    public void fromApi(Object slotApi)
    {
        Interval interval = (Interval) slotApi;
        setSlot(interval);
    }

    @Override
    public boolean equalsApi(Object slotApi)
    {
        if (slotApi instanceof Interval) {
            Interval interval = (Interval) slotApi;
            Interval slot = getSlot();
            return cz.cesnet.shongo.Temporal.isIntervalEqualed(interval, slot);
        }
        return false;
    }

    @Override
    public DateTimeSlot clone() throws CloneNotSupportedException
    {
        AbsoluteDateTimeSlot absoluteDateTimeSlot = (AbsoluteDateTimeSlot) super.clone();
        absoluteDateTimeSlot.setSlotStart(slotStart);
        absoluteDateTimeSlot.setSlotEnd(slotEnd);
        return absoluteDateTimeSlot;
    }
}
