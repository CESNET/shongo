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
    private String entityIdentifier;

    /**
     * Constructor.
     */
    public EntityNotFoundException()
    {
    }

    /**
     * Constructor.
     *
     * @param entityType       sets the {@link #entityType}
     * @param entityIdentifier sets the {@link #entityIdentifier}
     */
    public EntityNotFoundException(Class entityType, String entityIdentifier)
    {
        this.entityType = entityType;
        this.entityIdentifier = entityIdentifier;
    }

    /**
     * Constructor.
     *
     * @param entityType       sets the {@link #entityType}
     * @param entityIdentifier sets the {@link #entityIdentifier}
     */
    public EntityNotFoundException(Class entityType, Long entityIdentifier)
    {
        this(entityType, entityIdentifier.toString());
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
    public String getEntityIdentifier()
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
        return CommonFault.formatMessage("Entity '%s' with identifier '%s' doesn't exist.",
                entityType, entityIdentifier);
    }
}
