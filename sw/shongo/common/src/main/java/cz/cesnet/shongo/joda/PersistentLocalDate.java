package cz.cesnet.shongo.joda;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.DateType;
import org.hibernate.usertype.EnhancedUserType;
import org.joda.time.LocalDate;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Persist {@link org.joda.time.LocalDate} via hibernate.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistentLocalDate implements EnhancedUserType, Serializable
{
    public static final PersistentLocalDate INSTANCE = new PersistentLocalDate();

    private static final int[] SQL_TYPES = new int[]{Types.DATE,};

    @Override
    public int[] sqlTypes()
    {
        return SQL_TYPES;
    }

    @Override
    public Class returnedClass()
    {
        return LocalDate.class;
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
        LocalDate dtx = (LocalDate) x;
        LocalDate dty = (LocalDate) y;
        return dtx.equals(dty);
    }

    @Override
    public int hashCode(Object object) throws HibernateException
    {
        return object.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet resultSet, String[] names, SessionImplementor session, Object owner)
            throws HibernateException, SQLException
    {

        Object date = DateType.INSTANCE.nullSafeGet(resultSet, names, session, owner);
        if (date == null) {
            return null;
        }
        return new LocalDate(date);
    }

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index, SessionImplementor session)
            throws HibernateException, SQLException
    {
        if (value == null) {
            DateType.INSTANCE.nullSafeSet(preparedStatement, null, index, session);
        }
        else {
            DateType.INSTANCE.nullSafeSet(preparedStatement, ((LocalDate) value).toDateTimeAtStartOfDay().toDate(),
                    index, session);
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

    @Override
    public String objectToSQLString(Object object)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toXMLString(Object object)
    {
        return object.toString();
    }

    @Override
    public Object fromXMLString(String string)
    {
        return new LocalDate(string);
    }
}
