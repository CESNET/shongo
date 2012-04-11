package cz.cesnet.shongo.common.api;

/**
 * Represents an absolute date/time slot
 *
 * @author Martin Srom
 */
public class AbsoluteDateTimeSlot extends DateTimeSlot
{
    @Override
    public AbsoluteDateTime getStart()
    {
        DateTime dateTime = super.getStart();
        assert (dateTime instanceof AbsoluteDateTime) : "Absolute date/time slot should contain absolute date/time.";
        return (AbsoluteDateTime)dateTime;
    }

    @Override
    public void setStart(DateTime dateTime)
    {
        assert (dateTime instanceof AbsoluteDateTime) : "Absolute date/time slot should contain absolute date/time.";
        super.setStart(dateTime);
    }
}
