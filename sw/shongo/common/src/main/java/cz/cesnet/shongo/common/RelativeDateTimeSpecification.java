package cz.cesnet.shongo.common;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Period;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Represents an relative Date/Time. The relative date/time can be evaluated
 * only when concrete absolute date/time is given in getEarliest method.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RelativeDateTimeSpecification extends DateTimeSpecification
{
    private final Period duration;

    /**
     * Construct zero relative date/time.
     */
    public RelativeDateTimeSpecification()
    {
        duration = new Period();
    }

    /**
     * Construct relative date/time from current time.
     *
     * @param duration Relative date/time
     */
    public RelativeDateTimeSpecification(Period duration)
    {
        this.duration = duration;
    }

    /**
     * Construct relative date/time from current time.
     *
     * @param duration Relative date/time
     */
    public RelativeDateTimeSpecification(String duration)
    {
        this.duration = Period.parse(duration).normalizedStandard();
    }

    /**
     * Get duration for relative date/time.
     *
     * @return duration
     */
    @Column
    @Type(type = "Period")
    @Access(AccessType.FIELD)
    public Period getDuration()
    {
        return duration;
    }

    @Override
    public DateTime getEarliest(DateTime referenceDateTime)
    {
        return referenceDateTime.plus(getDuration());
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
        RelativeDateTimeSpecification relativeDateTime = (RelativeDateTimeSpecification) object;
        return getDuration().equals(relativeDateTime.getDuration());
    }
}
