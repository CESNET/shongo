package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.fault.FaultException;

/**
 * Extension of {@link cz.cesnet.shongo.controller.api.FaultSet}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerFaultSet extends cz.cesnet.shongo.controller.api.FaultSet
{
    /**
     * @return new instance of {@link cz.cesnet.shongo.api.FaultSet.EntityNotFoundFault}
     */
    public static <T> T throwEntityNotFoundFault(EntityIdentifier entityId) throws FaultException
    {
        return throwEntityNotFoundFault(entityId.getEntityClass().getSimpleName(), entityId.toId());
    }

    /**
     * @return new instance of {@link cz.cesnet.shongo.api.FaultSet.EntityNotFoundFault}
     */
    public static <T> T throwEntityNotFoundFault(Class entityType, Long entityId) throws FaultException
    {
        return throwEntityNotFoundFault(entityType.getSimpleName(),
                (EntityIdentifier.hasEntityType(entityType)
                         ? EntityIdentifier.formatId(entityType, entityId) : entityId.toString()));
    }

    /**
     * @return new instance of {@link cz.cesnet.shongo.api.FaultSet.EntityNotDeletableReferencedFault}
     */
    public static <T> T throwEntityNotDeletableReferencedFault(Class entityType, Long entityId) throws FaultException
    {
        return throwEntityNotDeletableReferencedFault(entityType.getSimpleName(),
                (EntityIdentifier.hasEntityType(entityType)
                         ? EntityIdentifier.formatId(entityType, entityId) : entityId.toString()));
    }

    /**
     * @return new instance of {@link SecurityNotAuthorizedFault}
     */
    public static <T> T throwSecurityNotAuthorizedFault(String action, Object... objects)
            throws FaultException
    {
        SecurityNotAuthorizedFault securityNotAuthorizedFault =
                createSecurityNotAuthorizedFault(String.format(action, objects));
        throw securityNotAuthorizedFault.createException();
    }
}
