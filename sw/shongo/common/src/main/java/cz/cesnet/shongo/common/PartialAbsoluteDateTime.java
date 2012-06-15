package cz.cesnet.shongo.common;

import javax.persistence.Entity;

/**
 * Represents an absolute Date/Time that have date and time which can be partially filled.
 *
 * @see PartialDate
 * @see PartialTime
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class PartialAbsoluteDateTime extends AbsoluteDateTime
{
    /**
     * Construct zero date/time.
     */
    public PartialAbsoluteDateTime()
    {
        this(new PartialDate(), new PartialTime());
    }

    /**
     * Construct date/time from an ISO8601 string, e.g. "2007-04-05T14:30:00".
     *
     * @param dateTime ISO8601 Date/Time;
     */
    public PartialAbsoluteDateTime(String dateTime)
    {
        super(dateTime);
    }

    /**
     * Construct date/time from an date and time objects. These objects became part
     * of date/time so when date/time is modified date and time objects become
     * modified too.
     *
     * @param date PatternDate object
     * @param time Time object
     */
    public PartialAbsoluteDateTime(PartialDate date, PartialTime time)
    {
        setDate(date);
        setTime(time);
    }

    @Override
    public PartialDate getDate()
    {
        return (PartialDate) super.getDate();
    }

    @Override
    public PartialTime getTime()
    {
        return (PartialTime) super.getTime();
    }

    @Override
    public PartialAbsoluteDateTime clone()
    {
        return new PartialAbsoluteDateTime(getDate().clone(), getTime().clone());
    }

    @Override
    public PartialAbsoluteDateTime add(Period period)
    {
        throw new RuntimeException("TODO: Implement");
    }

    @Override
    public PartialAbsoluteDateTime subtract(Period period)
    {
        throw new RuntimeException("TODO: Implement");
    }

    @Override
    public PartialAbsoluteDateTime merge(AbsoluteDateTime dateTime)
    {
        return new PartialAbsoluteDateTime(getDate().merge(dateTime.getDate()), getTime().merge(dateTime.getTime()));
    }
}
