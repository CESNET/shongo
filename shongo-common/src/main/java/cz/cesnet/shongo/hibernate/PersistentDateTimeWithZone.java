package cz.cesnet.shongo.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
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
public class PersistentDateTimeWithZone implements UserType<DateTime>, Serializable
{

    /**
     * Maximum database field length.
     */
    public static final int TIME_ZONE_LENGTH = PersistentDateTimeZone.LENGTH;

    private static final int[] SQL_TYPES = new int[]{Types.TIMESTAMP, Types.VARCHAR};

    @Override
    public int getSqlType()
    {
        return SQL_TYPES[0];
    }

    @Override
    public Class<DateTime> returnedClass()
    {
        return DateTime.class;
    }

    @Override
    public boolean equals(DateTime x, DateTime y) throws HibernateException
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
    public int hashCode(DateTime object) throws HibernateException
    {
        return object.hashCode();
    }

    @Override
    public DateTime nullSafeGet(ResultSet resultSet, int position, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException
    {
        Timestamp timestamp = resultSet.getTimestamp(position);
        String timezone = resultSet.getString(position + 1);
        if (timestamp == null || timezone == null) {
            return null;
        }
        return new DateTime(timestamp, DateTimeZone.forID(timezone));
    }

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, DateTime value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException
    {
        if (value == null) {
            preparedStatement.setNull(index, Types.TIMESTAMP);
            preparedStatement.setNull(index + 1, Types.VARCHAR);
        } else {
            DateTime dateTime = value;
            String timeZoneId = dateTime.getZone().getID();
            preparedStatement.setTimestamp(index, new Timestamp(dateTime.getMillis()));
//            preparedStatement.setTimestamp(index, dateTime.toDate());
            preparedStatement.setString(index + 1, timeZoneId);
        }
    }

    @Override
    public DateTime deepCopy(DateTime value) throws HibernateException
    {
        return value;
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }

    @Override
    public Serializable disassemble(DateTime value) throws HibernateException
    {
        return value;
    }

    @Override
    public DateTime assemble(Serializable cached, Object value) throws HibernateException
    {
        return (DateTime) cached;
    }

    @Override
    public DateTime replace(DateTime original, DateTime target, Object owner) throws HibernateException
    {
        return original;
    }
}

