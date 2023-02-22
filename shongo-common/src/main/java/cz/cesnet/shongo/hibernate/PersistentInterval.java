package cz.cesnet.shongo.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Persist {@link org.joda.time.Interval} via hibernate.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistentInterval implements CompositeUserType, Serializable
{
    /**
     * Name for {@link org.hibernate.annotations.TypeDef}.
     */
    public static final String NAME = "Interval";

    private static final String[] PROPERTY_NAMES = new String[]{"start", "end"};

    private static final Type[] TYPES = new Type[]{StandardBasicTypes.TIMESTAMP, StandardBasicTypes.TIMESTAMP};

    @Override
    public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException
    {
        return cached;
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException
    {
        return value;
    }

    @Override
    public Serializable disassemble(Object value, SharedSessionContractImplementor session) throws HibernateException
    {
        return (Serializable) value;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException
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
    public String[] getPropertyNames()
    {
        return PROPERTY_NAMES;
    }

    @Override
    public Type[] getPropertyTypes()
    {
        return TYPES;
    }

    @Override
    public Object getPropertyValue(Object component, int property) throws HibernateException
    {
        Interval interval = (Interval) component;
        return (property == 0) ? interval.getStart().toDate() : interval.getEnd().toDate();
    }

    @Override
    public int hashCode(Object x) throws HibernateException
    {
        return x.hashCode();
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }


    @Override
    public Object nullSafeGet(ResultSet resultSet, String[] names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException
    {
        if (resultSet == null) {
            return null;
        }
        PersistentDateTime pst = new PersistentDateTime();
        DateTime start = (DateTime) pst.nullSafeGet(resultSet, new String[]{names[0]}, session, owner);
        DateTime end = (DateTime) pst.nullSafeGet(resultSet, new String[]{names[1]}, session, owner);
        if (start == null || end == null) {
            return null;
        }
        return new Interval(start, end);
    }

    @Override
    public void nullSafeSet(PreparedStatement statement, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException
    {
        if (value == null) {
            statement.setNull(index, StandardBasicTypes.TIMESTAMP.sqlType());
            statement.setNull(index + 1, StandardBasicTypes.TIMESTAMP.sqlType());
            return;
        }
        Interval interval = (Interval) value;
        statement.setTimestamp(index, asTimeStamp(interval.getStart()));
        statement.setTimestamp(index + 1, asTimeStamp(interval.getEnd()));
    }

    private Timestamp asTimeStamp(DateTime time)
    {
        return new Timestamp(time.getMillis());
    }

    @Override
    public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner)
            throws HibernateException
    {
        return original;
    }

    @Override
    public Class returnedClass()
    {
        return Interval.class;
    }

    @Override
    public void setPropertyValue(Object component, int property, Object value) throws HibernateException
    {
        throw new UnsupportedOperationException("Immutable Interval");
    }
}
