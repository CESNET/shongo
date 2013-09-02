package cz.cesnet.shongo.hibernate;

import cz.cesnet.shongo.api.Converter;
import org.hibernate.HibernateException;
import org.joda.time.DateTimeZone;

import java.util.Locale;

/**
 * Persist {@link org.joda.time.DateTimeZone} via hibernate.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistentLocale extends PersistentStringType
{
    public static final PersistentLocale INSTANCE = new PersistentLocale();

    @Override
    public Class returnedClass()
    {
        return Locale.class;
    }

    @Override
    protected Object fromNonNullString(String string) throws HibernateException
    {
        return Converter.convertStringToLocale(string);
    }

    @Override
    protected String toNonNullString(Object value) throws HibernateException
    {
        return Converter.convertLocaleToString((Locale) value);
    }
}

