package cz.cesnet.shongo.controller.rest.error;

/**
 * Object is inaccessible.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ObjectInaccessibleException extends RuntimeException
{
    private final String objectId;

    public ObjectInaccessibleException(String objectId)
    {
        this.objectId = objectId;
    }

    public String getObjectId()
    {
        return objectId;
    }
}
