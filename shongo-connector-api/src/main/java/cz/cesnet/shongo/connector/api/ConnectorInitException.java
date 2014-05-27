package cz.cesnet.shongo.connector.api;

/**
 * An exception thrown when initialization of a connector object fails.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ConnectorInitException extends RuntimeException
{
    public ConnectorInitException(String message)
    {
        super(message);
    }

    public ConnectorInitException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
