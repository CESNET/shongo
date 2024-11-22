package cz.cesnet.shongo.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.usertype.UserType;
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
public class PersistentDateTime implements UserType<DateTime>, Serializable
{

    private static final int SQL_TYPE = Types.TIMESTAMP;

    @Override
    public int getSqlType() {
        return SQL_TYPE;
    }

    @Override
    public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
        return dialect.getDefaultTimestampPrecision();
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
        if (timestamp == null) {
            return null;
        }
        return new DateTime(timestamp);
    }

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, DateTime value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException
    {
        if (value == null) {
            preparedStatement.setNull(index, Types.TIMESTAMP);
        }
        else {
            preparedStatement.setTimestamp(index, new Timestamp(value.getMillis()));
//            preparedStatement.setTimestamp(index, value.toDate());
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

