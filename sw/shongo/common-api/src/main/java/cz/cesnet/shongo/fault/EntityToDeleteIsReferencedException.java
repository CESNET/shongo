package cz.cesnet.shongo.fault;

/**
 * Exception to be thrown when an entity with an identifier cannot be deleted because it is still referenced.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class EntityToDeleteIsReferencedException extends EntityException
{
    /**
     * Constructor.
     */
    public EntityToDeleteIsReferencedException()
    {
    }

    /**
     * Constructor.
     *
     * @param entityType
     * @param entityId
     */
    public EntityToDeleteIsReferencedException(Class entityType, Long entityId)
    {
        super(entityType, entityId);
    }

    @Override
    public int getCode()
    {
        return CommonFault.ENTITY_TO_DELETE_IS_REFERENCED;
    }

    @Override
    public String getMessage()
    {
        return CommonFault.formatMessage("Entity '%s' with identifier '%s' cannot be deleted (it is still referenced).",
                getEntityType(), getEntityId());
    }
}
