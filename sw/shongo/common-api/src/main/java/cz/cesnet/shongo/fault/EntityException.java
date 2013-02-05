package cz.cesnet.shongo.fault;

/**
 * Exception to be thrown when an entity is referenced.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class EntityException extends FaultException
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
    public EntityException()
    {
    }

    /**
     * Constructor.
     *
     * @param entityType       sets the {@link #entityType}
     * @param entityId sets the {@link #entityId}
     */
    public EntityException(Class entityType, String entityId)
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
    public EntityException(Class entityType, Long entityId)
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

    /**
     * @param entityId sets the {@link #entityId}
     */
    public void setEntityId(String entityId)
    {
        this.entityId = entityId;
    }
}
