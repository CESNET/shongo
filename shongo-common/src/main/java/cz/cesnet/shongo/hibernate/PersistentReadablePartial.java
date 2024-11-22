package cz.cesnet.shongo.hibernate;

import cz.cesnet.shongo.api.Converter;
import org.hibernate.HibernateException;
import org.joda.time.ReadablePartial;

/**
 * Persist {@link org.joda.time.ReadablePartial} via hibernate.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistentReadablePartial extends PersistentStringType<ReadablePartial>
{

    /**
     * Maximum database field length.
     */
    public static final int LENGTH = Converter.READABLE_PARTIAL_MAXIMUM_LENGTH;

    @Override
    public Class<ReadablePartial> returnedClass()
    {
        return ReadablePartial.class;
    }

    @Override
    protected ReadablePartial fromNonNullString(String string) throws HibernateException
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
    protected String toNonNullString(ReadablePartial value) throws HibernateException
    {
        if (value != null) {
            return value.toString();
        }
        else {
            throw new HibernateException("Cannot save " + value.getClass().getName() + " as " +
                    ReadablePartial.class.getName() + ". Implement it if needed.");
        }
    }
}
