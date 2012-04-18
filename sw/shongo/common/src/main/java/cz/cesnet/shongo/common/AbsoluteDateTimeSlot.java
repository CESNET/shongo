package cz.cesnet.shongo.common;

/**
 * Represents an absolute date/time slot.
 *
 * @author Martin Srom
 */
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
    public AbsoluteDateTime getEnd()
    {
        if (end == null) {
            end = getStart().add(getDuration());
        }
        return end;
    }

    @Override
    public AbsoluteDateTime getStart()
    {
        DateTime dateTime = super.getStart();
        assert (dateTime instanceof AbsoluteDateTime) : "Absolute date/time slot should contain absolute date/time.";
        return (AbsoluteDateTime) dateTime;
    }
}
