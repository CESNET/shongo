package cz.cesnet.shongo.oldapi.util;

/**
 * {@link RuntimeException} thrown from {@link cz.cesnet.shongo.oldapi.util.Converter}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ConverterException extends RuntimeException
{
    public ConverterException(Throwable throwable, String message, Object... objects)
    {
        super(String.format(message, objects), throwable);
    }

    public ConverterException(String message, Object... objects)
    {
        super(String.format(message, objects));
    }
}
