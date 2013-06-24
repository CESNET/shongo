package cz.cesnet.shongo.api.util;

/**
 * {@link RuntimeException} thrown from {@link Property}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PropertyException extends RuntimeException
{
    public PropertyException(Throwable throwable, String message, Object... objects)
    {
        super(String.format(message, objects), throwable);
    }

    public PropertyException(String message, Object... objects)
    {
        super(String.format(message, objects));
    }
}
