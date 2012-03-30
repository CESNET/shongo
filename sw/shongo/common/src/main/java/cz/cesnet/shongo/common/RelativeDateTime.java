package cz.cesnet.shongo.common;

/**
 * Represents an relative Date/Time from now.
 *
 * @author Martin Srom
 */
public class RelativeDateTime extends DateTime
{
    private AbsoluteDateTime referenceDateTime;

    private Period duration;

    /**
     * Construct relative date/time from current time.
     *
     * @param duration    Relative date/time
     */
    public RelativeDateTime(Period duration)
    {
        this(null, duration);
    }

    /**
     * Construct relative date/time from specified referenceDateTime.
     *
     * @param referenceDateTime    Base date/time, can be null
     * @param duration             Relative data/time
     */
    public RelativeDateTime(AbsoluteDateTime referenceDateTime, Period duration)
    {
        setReferenceDateTime(referenceDateTime);
        setDuration(duration);
    }

    /**
     * Set base date/time from which is relative calculated.
     *
     * @param referenceDateTime    Base date/time, can be null
     */
    public void setReferenceDateTime(AbsoluteDateTime referenceDateTime)
    {
        this.referenceDateTime = referenceDateTime;
    }

    /**
     * Get duration for relative date/time
     *
     * @return duration
     */
    public Period getDuration()
    {
        return duration;
    }

    /**
     * Set duration for relative date/time
     *
     * @param duration
     */
    public void setDuration(Period duration)
    {
        this.duration = duration;
    }

    /**
     * Get the earliest Date/Time.
     *
     * @return absolute Date/Time
     */
    @Override
    public AbsoluteDateTime getEarliest()
    {
        if (referenceDateTime == null) {
            return DateTime.now().add(getDuration());
        }
        else {
            return referenceDateTime.add(getDuration());
        }
    }
}
