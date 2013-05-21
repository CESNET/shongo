package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.controller.common.EntityIdentifier;

/**
 * Extensions for {@link CommonReportSet} and {@link ControllerReportSet}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerReportSetHelper
{
    /**
     * @throws {@link CommonReportSet.EntityNotFoundException}
     */
    public static <T> T throwEntityNotFoundFault(EntityIdentifier entityId)
            throws CommonReportSet.EntityNotFoundException
    {
        throw new CommonReportSet.EntityNotFoundException(entityId.getEntityClass().getSimpleName(), entityId.toId());
    }

    /**
     * @throws {@link CommonReportSet.EntityNotFoundException}
     */
    public static <T> T throwEntityNotFoundFault(Class entityType, Long entityId)
            throws CommonReportSet.EntityNotFoundException
    {
        throw new CommonReportSet.EntityNotFoundException(entityType.getSimpleName(),
                (EntityIdentifier.hasEntityType(entityType)
                         ? EntityIdentifier.formatId(entityType, entityId) : entityId.toString()));
    }

    /**
     * @throws {@link CommonReportSet.EntityNotDeletableReferencedException}
     */
    public static <T> T throwEntityNotDeletableReferencedFault(Class entityType, Long entityId)
            throws CommonReportSet.EntityNotDeletableReferencedException
    {
        throw new CommonReportSet.EntityNotDeletableReferencedException(entityType.getSimpleName(),
                (EntityIdentifier.hasEntityType(entityType)
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
