package cz.cesnet.shongo.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserTypeLegacyBridge;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * Persist {@link org.joda.time.DateTime} via hibernate.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistentDateTime extends UserTypeLegacyBridge implements Serializable
{
    /**
     * Name for {@link org.hibernate.annotations.TypeDef}.
     */
    public static final String NAME = "DateTime";

    public static final PersistentDateTime INSTANCE = new PersistentDateTime();

    @Override
    public Class returnedClass()
    {
        return DateTime.class;
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
        DateTime dtx = (DateTime) x;
        DateTime dty = (DateTime) y;
        return dtx.equals(dty);
    }

    @Override
    public int hashCode(Object object) throws HibernateException
    {
        return object.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet resultSet, int position, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException
    {
        Timestamp timestamp = resultSet.getTimestamp(position);
        if (timestamp == null) {
            return null;
        }
        DateTime dateTime = new DateTime(timestamp);
        return dateTime;
    }

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException
    {
        if (value == null) {
            preparedStatement.setNull(index, Types.TIMESTAMP);
        }
        else {
            DateTime dateTime = (DateTime) value;
            preparedStatement.setTimestamp(index, new Timestamp(dateTime.getMillis()));
//            preparedStatement.setTimestamp(index, dateTime.toDate());
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException
    {
        return value;
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException
    {
        return (Serializable) value;
    }

    @Override
    public Object assemble(Serializable cached, Object value) throws HibernateException
    {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException
    {
        return original;
    }
}

