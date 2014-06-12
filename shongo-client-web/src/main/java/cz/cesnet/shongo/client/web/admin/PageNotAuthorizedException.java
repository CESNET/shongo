package cz.cesnet.shongo.client.web.admin;

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
