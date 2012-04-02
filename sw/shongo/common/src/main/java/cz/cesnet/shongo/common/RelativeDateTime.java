package cz.cesnet.shongo.common;

/**
 * Represents an relative Date/Time. The relative date/time can be evaluated
 * only when concrete absolute date/time is given in getEarliest method.
 *
 * @author Martin Srom
 */
public class RelativeDateTime extends DateTime
{
    private Period duration;

    /**
     * Construct zero relative date/time.
     */
    public RelativeDateTime()
    {
        this(new Period());
    }

    /**
     * Construct relative date/time from current time.
     *
     * @param duration    Relative date/time
     */
    public RelativeDateTime(Period duration)
    {
        setDuration(duration);
    }

    /**
     * Get duration for relative date/time.
     *
     * @return duration
     */
    public Period getDuration()
    {
        return duration;
    }

    /**
     * Set duration for relative date/time.
     *
     * @param duration
     */
    public void setDuration(Period duration)
    {
        this.duration = duration;
    }

    @Override
    public AbsoluteDateTime getEarliest(AbsoluteDateTime referenceDateTime)
    {
        return referenceDateTime.add(getDuration());
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
        RelativeDateTime relativeDateTime = (RelativeDateTime)object;
        return getDuration().equals(relativeDateTime.getDuration());
    }
}
