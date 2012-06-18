package cz.cesnet.shongo.common.joda;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.TimestampType;
import org.hibernate.usertype.EnhancedUserType;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Persist {@link org.joda.time.DateTime} via hibernate.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistentDateTime implements EnhancedUserType, Serializable
{
    public static final PersistentDateTime INSTANCE = new PersistentDateTime();

    private static final int[] SQL_TYPES = new int[]{Types.TIMESTAMP,};

    public int[] sqlTypes()
    {
        return SQL_TYPES;
    }

    public Class returnedClass()
    {
        return DateTime.class;
    }

    public boolean equals(Object x, Object y) throws HibernateException
    {
        if (x == y) {
            return true;
        }
        if (x == null || y == null) {
            return false;
        }
        DateTime dtx = (DateTime) x;
        DateTime dty = (DateTime) y;

        return dtx.equals(dty);
    }

    public int hashCode(Object object) throws HibernateException
    {
        return object.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet resultSet, String[] names, SessionImplementor session, Object owner)
            throws HibernateException, SQLException
    {
        Object timestamp = TimestampType.INSTANCE.nullSafeGet(resultSet, names, session, owner);
        if (timestamp == null) {
            return null;
        }
        return new DateTime(timestamp);
    }

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index, SessionImplementor session)
            throws HibernateException, SQLException
    {
        if (value == null) {
            TimestampType.INSTANCE.nullSafeSet(preparedStatement, null, index, session);
        }
        else {
            TimestampType.INSTANCE.nullSafeSet(preparedStatement, ((DateTime) value).toDate(), index, session);
        }
    }

    public Object deepCopy(Object value) throws HibernateException
    {
        return value;
    }

    public boolean isMutable()
    {
        return false;
    }

    public Serializable disassemble(Object value) throws HibernateException
    {
        return (Serializable) value;
    }

    public Object assemble(Serializable cached, Object value) throws HibernateException
    {
        return cached;
    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException
    {
        return original;
    }

    public String objectToSQLString(Object object)
    {
        throw new UnsupportedOperationException();
    }

    public String toXMLString(Object object)
    {
        return object.toString();
    }

    public Object fromXMLString(String string)
    {
        return new DateTime(string);
    }

}

