package cz.cesnet.shongo.hibernate;

import cz.cesnet.shongo.api.Converter;
import org.hibernate.HibernateException;
import org.joda.time.ReadablePartial;

/**
 * Persist {@link org.joda.time.ReadablePartial} via hibernate.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistentReadablePartial extends PersistentStringType
{
    /**
     * Name for {@link org.hibernate.annotations.TypeDef}.
     */
    public static final String NAME = "ReadablePartial";

    /**
     * Maximum database field length.
     */
    public static final int LENGTH = Converter.READABLE_PARTIAL_MAXIMUM_LENGTH;

    public static final PersistentReadablePartial INSTANCE = new PersistentReadablePartial();

    @Override
    public Class returnedClass()
    {
        return ReadablePartial.class;
    }

    @Override
    protected Object fromNonNullString(String string) throws HibernateException
    {
        try {
            return Converter.convertStringToReadablePartial(string);
        }
        catch (Exception exception) {
            throw new HibernateException("Failed to load " + ReadablePartial.class.getName() + " from '" +
                    string + "'", exception);
        }
    }

    @Override
    protected String toNonNullString(Object value) throws HibernateException
    {
        if (value instanceof ReadablePartial) {
            return value.toString();
        }
        else {
            throw new HibernateException("Cannot save " + value.getClass().getName() + " as " +
                    ReadablePartial.class.getName() + ". Implement it if needed.");
        }
    }
}
