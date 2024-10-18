package cz.cesnet.shongo.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.usertype.UserTypeLegacyBridge;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * Persist {@link org.joda.time.DateTime} with {@link org.joda.time.DateTimeZone} via hibernate.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistentDateTimeWithZone extends UserTypeLegacyBridge implements Serializable
{
    /**
     * Name for {@link org.hibernate.annotations.TypeDef}.
     */
    public static final String NAME = "DateTimeWithZone";

    /**
     * Maximum database field length.
     */
    public static final int TIME_ZONE_LENGTH = PersistentDateTimeZone.LENGTH;

    public static final PersistentDateTimeWithZone INSTANCE = new PersistentDateTimeWithZone();

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
        String timezone = resultSet.getString(position + 1);
        if (timestamp == null || timezone == null) {
            return null;
        }
        DateTime dateTime = new DateTime(timestamp, DateTimeZone.forID(timezone));
        return dateTime;
    }

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException
    {
        if (value == null) {
            preparedStatement.setNull(index, Types.TIMESTAMP);
            preparedStatement.setNull(index + 1, Types.VARCHAR);
        } else {
            DateTime dateTime = (DateTime) value;
            String timeZoneId = dateTime.getZone().getID();
            preparedStatement.setTimestamp(index, new Timestamp(dateTime.getMillis()));
//            preparedStatement.setTimestamp(index, dateTime.toDate());
            preparedStatement.setString(index + 1, timeZoneId);
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

