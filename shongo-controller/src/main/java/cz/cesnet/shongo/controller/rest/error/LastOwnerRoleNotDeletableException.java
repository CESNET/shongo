package cz.cesnet.shongo.controller.rest.error;

public class LastOwnerRoleNotDeletableException extends RuntimeException
{
    public LastOwnerRoleNotDeletableException()
    {
        super("Last OWNER role cannot be deleted.");
    }
}
