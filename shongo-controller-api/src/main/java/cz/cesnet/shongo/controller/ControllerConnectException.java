package cz.cesnet.shongo.controller;

import java.net.URL;

/**
 * Cannot connect to controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerConnectException extends RuntimeException
{
    /**
     * Controller URL.
     */
    private URL url;

    /**
     * Constructor.
     *
     * @param url
     * @param cause
     */
    public ControllerConnectException(URL url, Throwable cause)
    {
        super(cause);
        this.url = url;
    }

    @Override
    public String getMessage()
    {
        return String.format("Cannot connect to controller %s: %s", url, super.getMessage());
    }
}
