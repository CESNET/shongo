package cz.cesnet.shongo.controller.booking.datetime;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.hibernate.PersistentDateTime;
import cz.cesnet.shongo.hibernate.PersistentPeriod;
import org.hibernate.annotations.Parameter;
import org.hibernate.usertype.UserTypeLegacyBridge;
import org.joda.time.DateTime;
import org.joda.time.Period;

import jakarta.persistence.*;

/**
 * Represents a specification of absolute or relative date/time.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class DateTimeSpecification extends SimplePersistentObject
{
    /**
     * @see Type
     */
    private Type type;

    /**
     * {@link Type#ABSOLUTE} data.
     */
    private DateTime absoluteDateTime;

    /**
     * {@link Type#RELATIVE} data.
     */
    private Period relativeDateTime;

    /**
     * Constructor.
     */
    private DateTimeSpecification()
    {
    }

    /**
     * @return {@link #type}
     */
    @Column(length = AbstractComplexType.ENUM_COLUMN_LENGTH)
    @Enumerated(EnumType.STRING)
    public Type getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(Type type)
    {
        this.type = type;
    }

    /**
     * @return {@link #absoluteDateTime}
     */
    @Column
    @org.hibernate.annotations.Type(PersistentDateTime.class)
    @Access(AccessType.FIELD)
    public DateTime getAbsoluteDateTime()
    {
        return absoluteDateTime;
    }

    /**
     * @param absoluteDateTime sets the {@link #absoluteDateTime}
     */
    public void setAbsoluteDateTime(DateTime absoluteDateTime)
    {
        this.absoluteDateTime = absoluteDateTime;
    }

    /**
     * @return {@link #relativeDateTime}
     */
    @Column(length = PersistentPeriod.LENGTH)
    @org.hibernate.annotations.Type(PersistentPeriod.class)
    @Access(AccessType.FIELD)
    public Period getRelativeDateTime()
    {
        return relativeDateTime;
    }

    /**
     * @param relativeDateTime sets the {@link #relativeDateTime}
     */
    public void setRelativeDateTime(Period relativeDateTime)
    {
        this.relativeDateTime = relativeDateTime;
    }

    /**
     * Get the earliest Date/Time since a given datetime (strict inequality).
     *
     * @param referenceDateTime the datetime since which to find the earliest occurrence
     * @return absolute Date/Time, or <code>null</code> if the datetime won't take place since referenceDateTime
     */
    @Transient
    public DateTime getEarliest(DateTime referenceDateTime)
    {
        switch (type) {
            case ABSOLUTE:
                if (referenceDateTime == null || absoluteDateTime.isAfter(referenceDateTime)) {
                    return absoluteDateTime;
                }
                return null;
            case RELATIVE:
                return referenceDateTime.plus(relativeDateTime);
            default:
                throw new TodoImplementException(type);
        }
    }

    /**
     * @return api object
     */
    public Object toApi()
    {
        switch (type) {
            case ABSOLUTE:
                return absoluteDateTime;
            case RELATIVE:
                return relativeDateTime;
            default:
                throw new TodoImplementException(type);
        }
    }

    /**
     * @param api                   from which should be the {@code dateTimeSpecification} filled
     * @param dateTimeSpecification to be used or null to create new
     * @return new or modified {@code dateTimeSpecification}
     */
    public static DateTimeSpecification fromApi(Object api, DateTimeSpecification dateTimeSpecification)
    {
        if (dateTimeSpecification == null) {
            dateTimeSpecification = new DateTimeSpecification();
        }
        if (api instanceof DateTime) {
            dateTimeSpecification.setType(Type.ABSOLUTE);
            dateTimeSpecification.setAbsoluteDateTime((DateTime) api);
        }
        else if (api instanceof Period) {
            dateTimeSpecification.setType(Type.RELATIVE);
            dateTimeSpecification.setRelativeDateTime((Period) api);
        }
        else {
            throw new TodoImplementException(api.getClass());
        }
        return dateTimeSpecification;
    }

    /**
     * @param dateTimeSpecification absolute date/time or period (relative date/time)
     * @return new instance of {@link DateTimeSpecification} for given {@code dateTimeSpecification}
     */
    public static DateTimeSpecification fromString(String dateTimeSpecification)
    {
        try {
            return fromApi(DateTime.parse(dateTimeSpecification), null);
        }
        catch (IllegalArgumentException exception) {
            return fromApi(Period.parse(dateTimeSpecification), null);
        }
    }

    /**
     * Type of date/time which is specified by the {@link DateTimeSpecification}.
     */
    public static enum Type
    {
        /**
         * Represents an absolute date/time which is full date/time definition.
         */
        ABSOLUTE,

        /**
         * Represents an relative date/time which is period definition which is added to the current date/time when
         * it should be evaluated.
         */
        RELATIVE
    }

}
