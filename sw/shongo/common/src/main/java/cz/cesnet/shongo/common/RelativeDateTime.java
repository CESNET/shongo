package cz.cesnet.shongo.common;

import javax.persistence.*;

/**
 * Represents an relative Date/Time. The relative date/time can be evaluated
 * only when concrete absolute date/time is given in getEarliest method.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RelativeDateTime extends DateTime
{
    private final Period duration;

    /**
     * Construct zero relative date/time.
     */
    public RelativeDateTime()
    {
        duration = new Period();
    }

    /**
     * Construct relative date/time from current time.
     *
     * @param duration Relative date/time
     */
    public RelativeDateTime(Period duration)
    {
        this.duration = duration;
    }

    /**
     * Get duration for relative date/time.
     *
     * @return duration
     */
    @Column
    @Access(AccessType.FIELD)
    public Period getDuration()
    {
        return duration;
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
        RelativeDateTime relativeDateTime = (RelativeDateTime) object;
        return getDuration().equals(relativeDateTime.getDuration());
    }
}
