package cz.cesnet.shongo.fault.old;

/**
 * Exception to be thrown when an entity validation failed.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class EntityValidationException extends OldFaultException implements SerializableException
{
    /**
     * Constructor.
     *
     * @param entityType
     * @param entityId
     */
    public EntityValidationException(Class entityType, Long entityId, String format, Object... objects)
    {
        super(CommonFault.ENTITY_VALIDATION, "%s (entity '%s' with identifier '%d')",
                String.format(format, objects), entityType, entityId);
    }
}
