package cz.cesnet.shongo.controller.booking.datetime;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.hibernate.PersistentPeriod;
import org.hibernate.annotations.Type;
import org.hibernate.usertype.UserTypeLegacyBridge;
import org.joda.time.*;

import jakarta.persistence.*;
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
    @Column(length = PersistentPeriod.LENGTH)
    @Type(value = PersistentPeriod.class, parameters = {@org.hibernate.annotations.Parameter(name = UserTypeLegacyBridge.TYPE_NAME_PARAM_KEY, value = "PersistentPeriod")})
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
     * Conbstructor
     *
     * @param slot interval
     * @param period period of slot
     * @param end end of interval period
     */
//    public PeriodicDateTimeSlot(Interval slot, Period period, ReadablePartial end)
//    {
//        this.periodicDateTime = new PeriodicDateTime(slot.getStart(),period,end);
//        this.duration = slot.toPeriod();
//    }

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
        Period duration = getDuration();
        for (DateTime dateTime : periodicDateTime.enumerate(
                (intervalStart != null ? intervalStart.minus(duration) : null), intervalEnd, maxCount - slots.size())) {
            Interval slot = new Interval(dateTime, duration);
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
    public boolean contains(Interval slot) {
        List<Interval> slots = enumerate(slot.getStart(), slot.getEnd(), 1);

        if (slots.size() == 1) {
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
        periodicDateTimeSlotApi.setTimeZone(periodicDateTime.getTimeZone());
        periodicDateTimeSlotApi.setDuration(getDuration());
        periodicDateTimeSlotApi.setPeriod(periodicDateTime.getPeriod());
        periodicDateTimeSlotApi.setEnd(periodicDateTime.getEnd());
        periodicDateTimeSlotApi.setPeriodicityDayOrder(periodicDateTime.getPeriodicityDayOrder());
        periodicDateTimeSlotApi.setPeriodicityDayInMonth(periodicDateTime.getPeriodicityDayInMonth());
        if (periodicDateTime.getPeriodicityDayOrder() != null && periodicDateTime.getPeriodicityDayInMonth() != null) {
            periodicDateTimeSlotApi.setMonthPeriodicityType(cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType.SPECIFIC_DAY);
        }
        else {
            periodicDateTimeSlotApi.setMonthPeriodicityType(cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType.STANDARD);
        }

        for (PeriodicDateTime.Rule rule : periodicDateTime.getRules()) {
            if (rule.isInterval()) {
                throw new TodoImplementException("Interval rules are not implemented yet.");
            }
            switch (rule.getType()) {
                case DISABLE:
                    periodicDateTimeSlotApi.addExcludeDate(rule.getDateTime());
                    break;
                default:
                    throw new TodoImplementException("Unsupported rule type: " + rule.getType());
            }
        }

        return periodicDateTimeSlotApi;
    }

    @Override
    public void fromApi(Object slotApi)
    {
        cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot periodicDateTimeSlotApi =
                (cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot) slotApi;
        PeriodicDateTime periodicDateTime = new PeriodicDateTime();
        periodicDateTime.setStart(periodicDateTimeSlotApi.getStart());
        periodicDateTime.setTimeZone(periodicDateTimeSlotApi.getTimeZone());
        periodicDateTime.setPeriod(periodicDateTimeSlotApi.getPeriod());
        periodicDateTime.setEnd(periodicDateTimeSlotApi.getEnd());
        if (cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType.SPECIFIC_DAY.equals(periodicDateTimeSlotApi.getMonthPeriodicityType())) {
            periodicDateTime.setPeriodicityDayOrder(periodicDateTimeSlotApi.getPeriodicityDayOrder());
            periodicDateTime.setPeriodicityDayInMonth(periodicDateTimeSlotApi.getPeriodicityDayInMonth());
        } else {
            periodicDateTime.setPeriodicityDayOrder(null);
            periodicDateTime.setPeriodicityDayInMonth(null);
        }
        setPeriodicDateTime(periodicDateTime);
        setDuration(periodicDateTimeSlotApi.getDuration());

        periodicDateTime.addAllRules(PeriodicDateTime.RuleType.DISABLE, periodicDateTimeSlotApi.getExcludeDates());
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
    public DateTimeSlot clone() throws CloneNotSupportedException
    {
        PeriodicDateTimeSlot periodicDateTimeSlot = (PeriodicDateTimeSlot) super.clone();
        periodicDateTimeSlot.setPeriodicDateTime(periodicDateTime.clone());
        periodicDateTimeSlot.setDuration(duration);
        return periodicDateTimeSlot;
    }
}
