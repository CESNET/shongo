package cz.cesnet.shongo.hibernate;

import cz.cesnet.shongo.api.Converter;
import org.hibernate.HibernateException;
import org.joda.time.Period;

/**
 * Persist {@link org.joda.time.Period} via hibernate.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistentPeriod extends PersistentStringType<Period>
{

    /**
     * Maximum database field length.
     */
    public static final int LENGTH = Converter.PERIOD_MAXIMUM_LENGTH;

    @Override
    public Class<Period> returnedClass()
    {
        return Period.class;
    }

    @Override
    protected Period fromNonNullString(String s) throws HibernateException
    {
        return new Period(s);
    }

    @Override
    protected String toNonNullString(Period value) throws HibernateException
    {
        return value.toString();
    }
}
