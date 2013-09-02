package cz.cesnet.shongo.joda;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.TimestampType;
import org.hibernate.usertype.UserType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Persist {@link org.joda.time.DateTimeZone} via hibernate.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistentDateTimeZone extends PersistentStringType
{
    public static final PersistentDateTimeZone INSTANCE = new PersistentDateTimeZone();

    @Override
    public Class returnedClass()
    {
        return DateTimeZone.class;
    }

    @Override
    protected Object fromNonNullString(String string) throws HibernateException
    {
        return DateTimeZone.forID(string);
    }

    @Override
    protected String toNonNullString(Object value) throws HibernateException
    {
        return ((DateTimeZone) value).getID();
    }
}

