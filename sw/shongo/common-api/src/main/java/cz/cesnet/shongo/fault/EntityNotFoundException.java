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
     *
     * @param entityType
     * @param entityIdentifier
     */
    public EntityNotFoundException(Class entityType, Long entityIdentifier)
    {
        super(CommonFault.ENTITY_NOT_FOUND, "Entity '%s' with identifier '%d' doesn't exist.",
                entityType, entityIdentifier);
        this.entityType = entityType;
        this.entityIdentifier = entityIdentifier;
    }

    /**
     * Constructor.
     *
     * @param message message containing parsed parameters
     */
    public EntityNotFoundException(Message message)
    {
        this(message.getParameterAsClass("entityType"), message.getParameterAsLong("entityIdentifier"));
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
}
