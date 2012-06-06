package cz.cesnet.shongo.common;

import javax.persistence.*;

/**
 * Represents an absolute date/time slot.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AbsoluteDateTimeSlot extends DateTimeSlot
{
    /**
     * Time slot end date/time.
     */
    private AbsoluteDateTime end;

    /**
     * Construct absolute date/time slot.
     *
     * @param dateTime
     * @param period
     */
    public AbsoluteDateTimeSlot(AbsoluteDateTime dateTime, Period period)
    {
        super(dateTime, period);
    }

    /**
     * Get date/time slot end.
     *
     * @return end date/time
     */
    @Transient
    public AbsoluteDateTime getEnd()
    {
        if (end == null) {
            end = getStart().add(getDuration());
        }
        return end;
    }

    @Transient
    @Override
    public AbsoluteDateTime getStart()
    {
        DateTime dateTime = super.getStart();
        if ( (dateTime instanceof AbsoluteDateTime) == false ) {
            throw new IllegalStateException("Absolute date/time slot should contain absolute date/time.");
        }
        return (AbsoluteDateTime) dateTime;
    }
}
