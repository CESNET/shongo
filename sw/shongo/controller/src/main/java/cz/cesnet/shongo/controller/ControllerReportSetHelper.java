package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.controller.booking.EntityIdentifier;

/**
 * Extensions for {@link CommonReportSet} and {@link ControllerReportSet}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerReportSetHelper
{
    /**
     * @throws {@link cz.cesnet.shongo.CommonReportSet.EntityNotExistsException}
     */
    public static <T> T throwEntityNotExistFault(EntityIdentifier entityId)
            throws CommonReportSet.EntityNotExistsException
    {
        throw new CommonReportSet.EntityNotExistsException(entityId.getEntityClass().getSimpleName(), entityId.toId());
    }

    /**
     * @throws {@link cz.cesnet.shongo.CommonReportSet.EntityNotExistsException}
     */
    public static <T> T throwEntityNotExistFault(Class entityType, Long entityId)
            throws CommonReportSet.EntityNotExistsException
    {
        throw new CommonReportSet.EntityNotExistsException(entityType.getSimpleName(),
                (EntityIdentifier.isAvailableForEntityType(entityType)
                         ? EntityIdentifier.formatId(entityType, entityId) : entityId.toString()));
    }

    /**
     * @throws {@link CommonReportSet.EntityNotDeletableReferencedException}
     */
    public static <T> T throwEntityNotDeletableReferencedFault(Class entityType, Long entityId)
            throws CommonReportSet.EntityNotDeletableReferencedException
    {
        throw new CommonReportSet.EntityNotDeletableReferencedException(entityType.getSimpleName(),
                (EntityIdentifier.isAvailableForEntityType(entityType)
                         ? EntityIdentifier.formatId(entityType, entityId) : entityId.toString()));
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
