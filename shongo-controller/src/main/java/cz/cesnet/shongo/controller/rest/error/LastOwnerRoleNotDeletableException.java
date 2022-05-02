package cz.cesnet.shongo.controller.rest.error;

/**
 * Last owner role is not deletable.
 *
 * @author Filip Karnis
 */
public class LastOwnerRoleNotDeletableException extends RuntimeException
{
    public LastOwnerRoleNotDeletableException()
    {
        super("Last OWNER role cannot be deleted.");
    }
}
