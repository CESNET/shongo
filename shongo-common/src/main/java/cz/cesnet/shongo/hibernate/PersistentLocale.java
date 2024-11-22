package cz.cesnet.shongo.hibernate;

import cz.cesnet.shongo.api.Converter;
import org.hibernate.HibernateException;

import java.util.Locale;

/**
 * Persist {@link java.util.Locale} via hibernate.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistentLocale extends PersistentStringType<Locale>
{

    /**
     * Maximum database field length.
     */
    public static final int LENGTH = Converter.LOCALE_MAXIMUM_LENGTH;

    @Override
    public Class<Locale> returnedClass()
    {
        return Locale.class;
    }

    @Override
    protected Locale fromNonNullString(String string) throws HibernateException
    {
        return Converter.convertStringToLocale(string);
    }

    @Override
    protected String toNonNullString(Locale value) throws HibernateException
    {
        return Converter.convertLocaleToString(value);
    }
}

