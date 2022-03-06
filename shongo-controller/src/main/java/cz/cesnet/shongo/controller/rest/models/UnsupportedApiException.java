package cz.cesnet.shongo.controller.rest.models;

/**
 * Unsupported API data.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UnsupportedApiException extends RuntimeException
{
    public UnsupportedApiException(Object object)
    {
        super(object.getClass().getCanonicalName());
    }

    public UnsupportedApiException(String message)
    {
        super(message);
    }

    public UnsupportedApiException(String message, Object... arguments)
    {
        super(String.format(message, arguments));
    }

    @Override
    public String getMessage()
    {
        return String.format("Not supported: %s", super.getMessage());
    }
}
