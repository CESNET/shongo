package cz.cesnet.shongo.common.joda;

import org.hibernate.HibernateException;
import org.joda.time.Period;

/**
 * Persist {@link org.joda.time.Period} via hibernate.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistentPeriod extends PersistentStringType
{
    public static final PersistentPeriod INSTANCE = new PersistentPeriod();

    @Override
    public Class returnedClass()
    {
        return Period.class;
    }

    @Override
    protected Object fromNonNullString(String s) throws HibernateException
    {
        return new Period(s);
    }

    @Override
    protected String toNonNullString(Object value) throws HibernateException
    {
        return value.toString();
    }
}
