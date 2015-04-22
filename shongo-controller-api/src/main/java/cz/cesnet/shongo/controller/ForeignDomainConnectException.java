package cz.cesnet.shongo.controller;

import java.net.URL;

/**
 * Cannot connect to foreign domain.
 *
 * @author: Ondřej Pavelka <pavelka@cesnet.cz>
 */
public class ForeignDomainConnectException extends RuntimeException
{
    /**
     * Action URL.
     */
    private String url;

    /**
     * Constructor.
     *
     * @param url
     * @param cause
     */
    public ForeignDomainConnectException(String url, Throwable cause)
    {
        super(cause);
        this.url = url;
    }

    public ForeignDomainConnectException(String url, String message)
    {
        super(message);
        this.url = url;
    }

    @Override
    public String getMessage()
    {
        return String.format("Cannot connect to foreign domain %s: %s", url, super.getMessage());
    }
}

