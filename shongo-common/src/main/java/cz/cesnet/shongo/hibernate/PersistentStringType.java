package cz.cesnet.shongo.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StringType;
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
public abstract class PersistentStringType implements UserType, Serializable
{
    private static final int[] SQL_TYPES = new int[]{Types.VARCHAR};

    @Override
    public int[] sqlTypes()
    {
        return SQL_TYPES;
    }

    protected abstract Object fromNonNullString(String string) throws HibernateException;

    @Override
    public Object nullSafeGet(ResultSet resultSet, String[] names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException
    {
        String string = (String) StringType.INSTANCE.nullSafeGet(resultSet, names, session, owner);
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
            StringType.INSTANCE.nullSafeSet(preparedStatement, null, index, session);
        }
        else {
            StringType.INSTANCE.nullSafeSet(preparedStatement, toNonNullString(value), index, session);
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
