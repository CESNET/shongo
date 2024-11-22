package cz.cesnet.shongo.hibernate;

        import cz.cesnet.shongo.TodoImplementException;
        import org.hibernate.HibernateException;
        import org.hibernate.engine.spi.SharedSessionContractImplementor;
        import org.hibernate.usertype.UserType;
        import org.joda.time.LocalDate;
        import org.joda.time.Partial;

        import java.io.Serializable;
        import java.sql.PreparedStatement;
        import java.sql.ResultSet;
        import java.sql.SQLException;
        import java.sql.Timestamp;
        import java.sql.Types;

/**
 * Persist {@link org.joda.time.LocalDate} via hibernate.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class PersistentLocalDate implements UserType<LocalDate>, Serializable
{

    private static final int SQL_TYPE = Types.TIMESTAMP;

    @Override
    public int getSqlType()
    {
        return SQL_TYPE;
    }

    @Override
    public Class<LocalDate> returnedClass()
    {
        return LocalDate.class;
    }

    @Override
    public boolean equals(LocalDate x, LocalDate y) throws HibernateException
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
    public int hashCode(LocalDate object) throws HibernateException
    {
        return object.hashCode();
    }

    @Override
    public LocalDate nullSafeGet(ResultSet resultSet, int position, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException
    {
        Timestamp timestamp = resultSet.getTimestamp(position);
        if (timestamp == null) {
            return null;
        }
        return new LocalDate(timestamp);
    }

    @Override
    public void nullSafeSet(PreparedStatement preparedStatement, LocalDate value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException
    {
        if (value == null) {
            preparedStatement.setNull(index, Types.TIMESTAMP);
        }
        else {
            LocalDate localDate;
//            if (value instanceof Partial) {
//                Partial partial = (Partial) value;
//                localDate = new LocalDate(partial);
//            } else if (value instanceof LocalDate) {
//                localDate = (LocalDate) value;
//            } else {
//                throw new TodoImplementException("Unsupported LocalDate instance.");
//            }

            preparedStatement.setTimestamp(index, new Timestamp(value.toDateTimeAtCurrentTime().getMillis()));
//            preparedStatement.setTimestamp(index, value.toDate());
        }
    }

    @Override
    public LocalDate deepCopy(LocalDate value) throws HibernateException
    {
        return value;
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }

    @Override
    public Serializable disassemble(LocalDate value) throws HibernateException
    {
        return value;
    }

    @Override
    public LocalDate assemble(Serializable cached, Object value) throws HibernateException
    {
        return (LocalDate) cached;
    }

    @Override
    public LocalDate replace(LocalDate original, LocalDate target, Object owner) throws HibernateException
    {
        return original;
    }
}
