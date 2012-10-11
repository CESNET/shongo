package cz.cesnet.shongo.fault;

/**
 * Exception to be thrown when an entity with an identifier hasn't been found.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class EntityNotFoundException extends FaultException
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
    public EntityNotFoundException()
    {
    }

    /**
     * Constructor.
     *
     * @param entityType
     * @param entityIdentifier
     */
    public EntityNotFoundException(Class entityType, Long entityIdentifier)
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
        return CommonFault.ENTITY_NOT_FOUND;
    }

    @Override
    public String getMessage()
    {
        return CommonFault.formatMessage("Entity '%s' with identifier '%d' doesn't exist.",
                entityType, entityIdentifier);
    }
}
