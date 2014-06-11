package cz.cesnet.shongo.client.web.admin;

/**
 * Not administrator exception.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NotAdministratorException extends RuntimeException
{
    public NotAdministratorException()
    {
        super("You are not authorized to view this page.");
    }
}
