package cz.cesnet.shongo.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Persist {@link org.joda.time.Interval} via hibernate.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistentInterval implements CompositeUserType<Interval>, Serializable
{
    /**
     * Name for {@link org.hibernate.annotations.TypeDef}.
     */
    public static final String NAME = "Interval";

    @Override
    public Interval assemble(Serializable cached, Object owner) throws HibernateException
    {
        return (Interval) cached;
    }

    @Override
    public Interval deepCopy(Interval value) throws HibernateException
    {
        return value;
    }

    @Override
    public Serializable disassemble(Interval value) throws HibernateException
    {
        return value;
    }

    @Override
    public boolean equals(Interval x, Interval y) throws HibernateException
    {
        if (x == y) {
            return true;
        }
        if (x == null || y == null) {
            return false;
        }
        return x.equals(y);
    }

    @Override
    public Object getPropertyValue(Interval interval, int property) throws HibernateException
    {
        return (property == 0) ? interval.getStart().toDate() : interval.getEnd().toDate();
    }

    @Override
    public Interval instantiate(ValueAccess valueAccess, SessionFactoryImplementor sessionFactoryImplementor) {
        DateTime start = valueAccess.getValue(0, DateTime.class);
        DateTime end = valueAccess.getValue(1, DateTime.class);

        if (start == null || end == null) {
            return null;
        }
        return new Interval(start, end);
    }

    @Override
    public Class<?> embeddable() {
        return IntervalEmbeddable.class;
    }

    @Override
    public int hashCode(Interval x) throws HibernateException
    {
        return x.hashCode();
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }

    private Timestamp asTimeStamp(DateTime time)
    {
        return new Timestamp(time.getMillis());
    }

    @Override
    public Interval replace(Interval original, Interval target, Object owner)
            throws HibernateException
    {
        return original;
    }

    @Override
    public Class<Interval> returnedClass()
    {
        return Interval.class;
    }

    public static class IntervalEmbeddable {
        private DateTime start;
        private DateTime end;
    }
}
