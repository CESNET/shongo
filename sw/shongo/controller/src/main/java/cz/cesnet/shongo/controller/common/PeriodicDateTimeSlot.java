package cz.cesnet.shongo.controller.common;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a {@link DateTimeSlot} for a {@link PeriodicDateTime}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class PeriodicDateTimeSlot extends DateTimeSlot
{
    /**
     * Starting date/time(s).
     */
    private PeriodicDateTime periodicDateTime;

    /**
     * Duration for the date/time slot(s).
     */
    private Period duration;

    /**
     * @return {@link #duration}
     */
    @Override
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
     * Constructor.
     */
    public PeriodicDateTimeSlot()
    {
    }

    /**
     * Constructor.
     *
     * @param periodicDateTime periodic date/time
     * @param duration         duration of the {@link DateTimeSlot} (e.g., two hours)
     */
    public PeriodicDateTimeSlot(PeriodicDateTime periodicDateTime, Period duration)
    {
        if (periodicDateTime == null) {
            throw new IllegalArgumentException("Date/time must not be null!");
        }
        if (duration == null) {
            throw new IllegalArgumentException("Duration must not be null!");
        }
        this.periodicDateTime = periodicDateTime;
        this.duration = duration;
    }

    /**
     * @return {@link #periodicDateTime}
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public PeriodicDateTime getPeriodicDateTime()
    {
        return periodicDateTime;
    }

    /**
     * @param periodicDateTime sets the {@link #periodicDateTime}
     */
    public void setPeriodicDateTime(PeriodicDateTime periodicDateTime)
    {
        this.periodicDateTime = periodicDateTime;
    }

    @Override
    protected List<Interval> enumerate(DateTime intervalStart, DateTime intervalEnd, int maxCount)
    {
        Interval interval = null;
        if (intervalStart != null && intervalEnd != null) {
            interval = new Interval(intervalStart, intervalEnd);
        }
        List<Interval> slots = new ArrayList<Interval>();
        for (DateTime dateTime : periodicDateTime.enumerate(intervalStart, intervalEnd, maxCount - slots.size())) {
            Interval slot = new Interval(dateTime, getDuration());
            if ((intervalStart == null && intervalEnd == null)
                    || (interval != null && slot.overlaps(interval))
                    || (intervalStart != null && intervalEnd == null && slot.getEnd().isAfter(intervalStart))
                    || (intervalStart == null && intervalEnd != null && slot.getStart().isBefore(intervalEnd))) {
                slots.add(slot);
            }
        }
        return slots;
    }

    @Override
    public Interval getEarliest(DateTime referenceDateTime)
    {
        DateTime dateTime = periodicDateTime.getEarliest(referenceDateTime.minus(duration));
        if (dateTime == null) {
            return null;
        }
        return new Interval(dateTime, duration);
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) {
            return true;
        }
        if (object == null || (object instanceof PeriodicDateTimeSlot) == false) {
            return false;
        }

        PeriodicDateTimeSlot periodicDateTimeSlot = (PeriodicDateTimeSlot) object;
        if (getId() != null) {
            return getId().equals(periodicDateTimeSlot.getId());
        }

        // TODO: think up how to do infinite equals
        List<Interval> slots1 = enumerate();
        List<Interval> slots2 = periodicDateTimeSlot.enumerate();
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
        result = 37 * result + periodicDateTime.hashCode();
        result = 37 * result + duration.hashCode();
        return result;
    }

    @Override
    public Object toApi()
    {
        cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot periodicDateTimeSlotApi =
                new cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot();
        PeriodicDateTime periodicDateTime = getPeriodicDateTime();
        periodicDateTimeSlotApi.setId(getId());
        periodicDateTimeSlotApi.setStart(periodicDateTime.getStart());
        periodicDateTimeSlotApi.setDuration(getDuration());
        periodicDateTimeSlotApi.setPeriod(periodicDateTime.getPeriod());
        periodicDateTimeSlotApi.setEnd(periodicDateTime.getEnd());
        return periodicDateTimeSlotApi;
    }

    @Override
    public void fromApi(Object slotApi)
    {
        cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot periodicDateTimeSlotApi =
                (cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot) slotApi;
        PeriodicDateTime periodicDateTimeSpecification = new PeriodicDateTime();
        periodicDateTimeSpecification.setStart(periodicDateTimeSlotApi.getStart());
        periodicDateTimeSpecification.setPeriod(periodicDateTimeSlotApi.getPeriod());
        periodicDateTimeSpecification.setEnd(periodicDateTimeSlotApi.getEnd());
        setPeriodicDateTime(periodicDateTimeSpecification);
        setDuration(periodicDateTimeSlotApi.getDuration());
    }

    @Override
    public boolean equalsApi(Object slotApi)
    {
        if (slotApi instanceof cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot) {
            cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot periodicDateTimeSlotApi =
                    (cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot) slotApi;
            return periodicDateTimeSlotApi.notNullIdAsLong().equals(getId());
        }
        return false;
    }

    @Override
    public DateTimeSlot clone()
    {
        PeriodicDateTimeSlot periodicDateTimeSlot = new PeriodicDateTimeSlot();
        periodicDateTimeSlot.setPeriodicDateTime(periodicDateTime.clone());
        periodicDateTimeSlot.setDuration(duration);
        return periodicDateTimeSlot;
    }
}
