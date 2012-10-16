package cz.cesnet.shongo.fault;

/**
 * Exception to be thrown when an entity with an identifier cannot be deleted because it is still referenced.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class EntityToDeleteIsReferencedException extends FaultException
{
    /**
     * Type of the entity.
     */
    private Class entityType;

    /**
     * Unique identifier of the entity.
     */
    private Long entityIdentifier;

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
     * @param entityIdentifier
     */
    public EntityToDeleteIsReferencedException(Class entityType, Long entityIdentifier)
    {
        this.entityType = entityType;
        this.entityIdentifier = entityIdentifier;
    }

    /**
     * @return {@link #entityType}
     */
    public Class getEntityType()
    {
        return entityType;
    }

    /**
     * @return {@link #entityIdentifier}
     */
    public Long getEntityIdentifier()
    {
        return entityIdentifier;
    }

    @Override
    public int getCode()
    {
        return CommonFault.ENTITY_TO_DELETE_IS_REFERENCED;
    }

    @Override
    public String getMessage()
    {
        return CommonFault.formatMessage("Entity '%s' with identifier '%d' cannot be deleted (it is still referenced).",
                entityType, entityIdentifier);
    }
}
