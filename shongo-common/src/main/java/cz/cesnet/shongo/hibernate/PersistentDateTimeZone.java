package cz.cesnet.shongo.hibernate;

import cz.cesnet.shongo.api.Converter;
import org.hibernate.HibernateException;
import org.joda.time.DateTimeZone;

/**
 * Persist {@link org.joda.time.DateTimeZone} via hibernate.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistentDateTimeZone extends PersistentStringType<DateTimeZone>
{

    /**
     * Maximum database field length.
     */
    public static final int LENGTH = Converter.DATE_TIME_ZONE_MAXIMUM_LENGTH;

    @Override
    public Class<DateTimeZone> returnedClass()
    {
        return DateTimeZone.class;
    }

    @Override
    protected DateTimeZone fromNonNullString(String string) throws HibernateException
    {
        return DateTimeZone.forID(string);
    }

    @Override
    protected String toNonNullString(DateTimeZone value) throws HibernateException
    {
        return value.getID();
    }
}

