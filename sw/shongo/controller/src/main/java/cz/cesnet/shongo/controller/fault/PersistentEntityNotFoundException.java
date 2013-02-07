package cz.cesnet.shongo.controller.fault;

import cz.cesnet.shongo.controller.common.IdentifierFormat;
import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.EntityNotFoundException;

/**
 * Exception to be thrown when an entity with an id hasn't been found.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersistentEntityNotFoundException extends EntityNotFoundException
{
    /**
     * Constructor.
     */
    public PersistentEntityNotFoundException()
    {
    }

    /**
     * Constructor.
     *
     * @param entityType sets the {@link #entityType}
     * @param entityId   sets the {@link #entityId}
     */
    public PersistentEntityNotFoundException(Class entityType, Long entityId)
    {
        super(entityType, (IdentifierFormat.hasEntityType(entityType) ?
                                   IdentifierFormat.formatGlobalId(entityType, entityId) : entityId.toString()));
    }

    @Override
    public int getCode()
    {
        return CommonFault.ENTITY_NOT_FOUND;
    }

    @Override
    public String getMessage()
    {
        return CommonFault.formatMessage("Entity '%s' with identifier '%s' doesn't exist.",
                getEntityType(), getEntityId());
    }
}
