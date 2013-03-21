package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.CommonFaultSet;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.fault.Fault;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.jade.CommandFailure;

/**
 * Extension of {@link ControllerFaultSet}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerImplFaultSet extends ControllerFaultSet
{
    /**
     * @return new instance of {@link CommonFaultSet.EntityNotFoundFault}
     */
    public static <T> T throwEntityNotFoundFault(EntityIdentifier entityId) throws FaultException
    {
        return throwEntityNotFoundFault(entityId.getEntityClass().getSimpleName(), entityId.toId());
    }

    /**
     * @return new instance of {@link CommonFaultSet.EntityNotFoundFault}
     */
    public static <T> T throwEntityNotFoundFault(Class entityType, Long entityId) throws FaultException
    {
        return throwEntityNotFoundFault(entityType.getSimpleName(),
                (EntityIdentifier.hasEntityType(entityType)
                         ? EntityIdentifier.formatId(entityType, entityId) : entityId.toString()));
    }

    /**
     * @return new instance of {@link CommonFaultSet.EntityNotDeletableReferencedFault}
     */
    public static <T> T throwEntityNotDeletableReferencedFault(Class entityType, Long entityId) throws FaultException
    {
        return throwEntityNotDeletableReferencedFault(entityType.getSimpleName(),
                (EntityIdentifier.hasEntityType(entityType)
                         ? EntityIdentifier.formatId(entityType, entityId) : entityId.toString()));
    }
}
