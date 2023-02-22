package cz.cesnet.shongo.hibernate;

        import cz.cesnet.shongo.TodoImplementException;
        import org.hibernate.HibernateException;
        import org.hibernate.engine.spi.SharedSessionContractImplementor;
        import org.hibernate.type.DateType;
        import org.hibernate.usertype.UserType;
        import org.joda.time.LocalDate;
        import org.joda.time.Partial;

        import java.io.Serializable;
        import java.sql.PreparedStatement;
        import java.sql.ResultSet;
        import java.sql.SQLException;
        import java.sql.Types;

/**
 * Persist {@link org.joda.time.LocalDate} via hibernate.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class PersistentLocalDate implements UserType, Serializable
{
    /**
     * Name for {@link org.hibernate.annotations.TypeDef}.
     */
    public static final String NAME = "LocalDate";

    public static final PersistentLocalDate INSTANCE = new PersistentLocalDate();

    private static final int[] SQL_TYPES = new int[]{Types.TIMESTAMP};

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
    public Object nullSafeGet(ResultSet resultSet, String[] names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException
    {
        Object timestamp = DateType.INSTANCE.nullSafeGet(resultSet, names, session, owner);
        if (timestamp == null) {
            return null;
        }
        LocalDate localDate = new LocalDate(timestamp);
        return localDate;
    }

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException
    {
        if (value == null) {
            DateType.INSTANCE.nullSafeSet(preparedStatement, null, index, session);
        }
        else {
            LocalDate localDate;
            if (value instanceof Partial) {
                Partial partial = (Partial) value;
                localDate = new LocalDate(partial);
            } else if (value instanceof LocalDate) {
                localDate = (LocalDate) value;
            } else {
                throw new TodoImplementException("Unsupported LocalDate instance.");
            }

            DateType.INSTANCE.nullSafeSet(preparedStatement, localDate.toDate(), index, session);
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