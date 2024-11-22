package cz.cesnet.shongo.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

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
public abstract class PersistentStringType<T> implements UserType<T>, Serializable
{

    protected abstract T fromNonNullString(String string) throws HibernateException;

    private static final int SQL_TYPE = Types.VARCHAR;

    @Override
    public int getSqlType()
    {
        return SQL_TYPE;
    }

    @Override
    public T nullSafeGet(ResultSet resultSet, int position, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException
    {
        String string = resultSet.getString(position);
        if (string == null) {
            return null;
        }
        return fromNonNullString(string);
    }

    protected abstract String toNonNullString(T value) throws HibernateException;

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, T value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException
    {
        if (value == null) {
            preparedStatement.setNull(index, Types.VARCHAR);
        }
        else {
            preparedStatement.setString(index, toNonNullString(value));
        }
    }

    @Override
    public boolean equals(T x, T y) throws HibernateException
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
    public int hashCode(T object) throws HibernateException
    {
        return object.hashCode();
    }

    @Override
    public T deepCopy(T value) throws HibernateException
    {
        return value;
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }

    @Override
    public Serializable disassemble(T value) throws HibernateException
    {
        return (Serializable) value;
    }

    @Override
    public T assemble(Serializable cached, Object value) throws HibernateException
    {
        return (T) cached;
    }

    @Override
    public T replace(T original, T target, Object owner) throws HibernateException
    {
        return original;
    }
}
