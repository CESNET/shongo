package cz.cesnet.shongo.fault;

/**
 * Exception to be thrown when an entity with an id hasn't been found.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class EntityNotFoundException extends EntityException
{
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
        super(entityType, entityId);
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
