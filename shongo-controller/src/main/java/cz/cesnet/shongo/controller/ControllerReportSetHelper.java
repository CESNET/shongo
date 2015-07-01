package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;

/**
 * Extensions for {@link CommonReportSet} and {@link ControllerReportSet}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerReportSetHelper
{
    /**
     * @throws {@link CommonReportSet.ObjectNotExistsException}
     */
    public static <T> T throwObjectNotExistFault(ObjectIdentifier objectId)
            throws CommonReportSet.ObjectNotExistsException
    {
        throw new CommonReportSet.ObjectNotExistsException(objectId.getObjectClass().getSimpleName(), objectId.toId());
    }

    /**
     * @throws {@link CommonReportSet.ObjectNotExistsException}
     */
    public static <T> T throwObjectNotExistFault(Class<? extends PersistentObject> objectType, Long objectId)
            throws CommonReportSet.ObjectNotExistsException
    {
        throw new CommonReportSet.ObjectNotExistsException(objectType.getSimpleName(),
                (ObjectIdentifier.isAvailableForObjectType(objectType)
                         ? ObjectIdentifier.formatId(objectType, objectId) : objectId.toString()));
    }

    /**
     * @throws {@link CommonReportSet.ObjectNotExistsException}
     */
    public static <T> T throwObjectNotExistFault(String domainName, Class<? extends PersistentObject> objectType, Long objectId)
            throws CommonReportSet.ObjectNotExistsException
    {
        throw new CommonReportSet.ObjectNotExistsException(objectType.getSimpleName(),
                (ObjectIdentifier.isAvailableForObjectType(objectType)
                        ? ObjectIdentifier.formatId(domainName, objectType, objectId) : objectId.toString()));
    }

    /**
     * @throws {@link CommonReportSet.ObjectNotDeletableReferencedException}
     */
    public static <T> T throwObjectNotDeletableReferencedFault(Class<? extends PersistentObject> objectType,
            Long objectId) throws CommonReportSet.ObjectNotDeletableReferencedException
    {
        throw new CommonReportSet.ObjectNotDeletableReferencedException(objectType.getSimpleName(),
                (ObjectIdentifier.isAvailableForObjectType(objectType)
                         ? ObjectIdentifier.formatId(objectType, objectId) : objectId.toString()));
    }

    /**
     * @throws {@link CommonReportSet.ObjectNotDeletableReferencedException}
     */
    public static <T> T throwObjectNotDeletableReferencedFault(Throwable e, Class<?> objectType, String objectId) throws CommonReportSet.ObjectNotDeletableReferencedException
    {
        throw new CommonReportSet.ObjectNotDeletableReferencedException(e, objectType.toString(), objectId);
    }

    /**
     * @throws {@link ControllerReportSet.SecurityNotAuthorizedException}
     */
    public static <T> T throwSecurityNotAuthorizedFault(String action, Object... objects)
            throws ControllerReportSet.SecurityNotAuthorizedException
    {
        throw new ControllerReportSet.SecurityNotAuthorizedException(String.format(action, objects));
    }
}
