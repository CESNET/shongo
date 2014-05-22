package cz.cesnet.shongo.hibernate;

import cz.cesnet.shongo.api.Converter;
import org.hibernate.HibernateException;
import org.joda.time.Period;

/**
 * Persist {@link org.joda.time.Period} via hibernate.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistentPeriod extends PersistentStringType
{
    /**
     * Name for {@link org.hibernate.annotations.TypeDef}.
     */
    public static final String NAME = "Period";

    /**
     * Maximum database field length.
     */
    public static final int LENGTH = Converter.PERIOD_MAXIMUM_LENGTH;

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
