package cz.cesnet.shongo.fault;

/**
 * Exception to be thrown when an entity with an id hasn't been found.
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
     * Id of the entity.
     */
    private String entityId;

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
     * @param entityId sets the {@link #entityId}
     */
    public EntityNotFoundException(Class entityType, String entityId)
    {
        this.entityType = entityType;
        this.entityId = entityId;
    }

    /**
     * Constructor.
     *
     * @param entityType       sets the {@link #entityType}
     * @param entityId sets the {@link #entityId}
     */
    public EntityNotFoundException(Class entityType, Long entityId)
    {
        this(entityType, entityId.toString());
    }

    /**
     * @return {@link #entityType}
     */
    public Class getEntityType()
    {
        return entityType;
    }

    /**
     * @return {@link #entityId}
     */
    public String getEntityId()
    {
        return entityId;
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
                entityType, entityId);
    }
}
