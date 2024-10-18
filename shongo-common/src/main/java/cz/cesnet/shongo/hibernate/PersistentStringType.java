package cz.cesnet.shongo.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserTypeLegacyBridge;

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
public abstract class PersistentStringType extends UserTypeLegacyBridge implements Serializable
{

    protected abstract Object fromNonNullString(String string) throws HibernateException;

    @Override
    public Object nullSafeGet(ResultSet resultSet, int position, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException
    {
        String string = resultSet.getString(position);
        if (string == null) {
            return null;
        }
        return fromNonNullString(string);
    }

    protected abstract String toNonNullString(Object value) throws HibernateException;

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index, SharedSessionContractImplementor session)
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
    public int hashCode(Object object) throws HibernateException
    {
        return object.hashCode();
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
