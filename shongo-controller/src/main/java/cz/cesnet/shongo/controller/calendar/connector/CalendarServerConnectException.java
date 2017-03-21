package cz.cesnet.shongo.controller.calendar.connector;

/**
 *
 *
 * @author Marek Perichta <mperichta@cesnet.cz>
 */

public class CalendarServerConnectException extends Exception
{
    /**
     * Action URL.
     */
    private String url;

    public CalendarServerConnectException(String url, Throwable cause)
    {
        super(cause);
        this.url = url;
    }


    public CalendarServerConnectException (String url, String message)
    {
        super(message);
        this.url = url;
    }

    @Override
    public String getMessage()
    {
        return String.format("Cannot connect to CalDAV server with url '%s' (URL: %s)", url, super.getMessage());
    }
}
