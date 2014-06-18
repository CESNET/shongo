package cz.cesnet.shongo.client.web;

/**
 * Not administrator exception.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PageNotAuthorizedException extends RuntimeException
{
    public PageNotAuthorizedException()
    {
        super("You are not authorized to view this page.");
    }
}
