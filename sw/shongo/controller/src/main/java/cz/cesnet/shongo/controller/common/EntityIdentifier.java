package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.EntityType;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.fault.IdentifierWrongDomainException;
import cz.cesnet.shongo.controller.fault.IdentifierWrongFormatException;
import cz.cesnet.shongo.controller.fault.IdentifierWrongTypeException;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.fault.TodoImplementException;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a container for entity identifier.
 * The class also contains methods for parsing and formatting global identifiers.
 * <p/>
 * Identifiers are of following format:
 * <p/>
 * {@code shongo:<domain>:<entity-type>:<entity-id>}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class EntityIdentifier
{
    /**
     * {@link EntityType} of the identifier.
     */
    private final EntityType entityType;

    /**
     * Identifier value.
     */
    private final Long persistenceId;

    /**
     * Constructor.
     *
     * @param entityType sets the {@link #entityType}
     * @param persistenceId   sets the {@link #persistenceId}
     */
    public EntityIdentifier(EntityType entityType, Long persistenceId)
    {
        this.entityType = entityType;
        this.persistenceId = persistenceId;
    }

    public EntityIdentifier(PersistentObject persistentObject)
    {
        this.entityType = getEntityType(persistentObject.getClass());
        this.persistenceId = persistentObject.getId();
    }

    /**
     * @return {@link #entityType}
     */
    public EntityType getEntityType()
    {
        return entityType;
    }

    /**
     * @return {@link #persistenceId}
     */
    public Long getPersistenceId()
    {
        return persistenceId;
    }

    /**
     * @return class for the {@link #entityType}
     */
    public Class<? extends  PersistentObject> getEntityClass()
    {
        return getEntityTypeClass(entityType);
    }

    @Override
    public int hashCode()
    {
        throw new TodoImplementException();
    }

    @Override
    public boolean equals(Object obj)
    {
        throw new TodoImplementException();
    }

    /**
     * Entity types by code.
     */
    private static final Map<String, EntityType> entityTypeByCode = new HashMap<String, EntityType>();

    /**
     * Entity types by class.
     */
    private static final Map<Class, EntityType> entityTypeByClass = new HashMap<Class, EntityType>();

    /**
     * Static initialization.
     */
    static {
        for (EntityType entityType : EntityType.class.getEnumConstants()) {
            entityTypeByCode.put(entityType.getCode(), entityType);
            entityTypeByClass.put(getEntityTypeClass(entityType), entityType);
        }
    }

    /**
     * @param entityClass entity type class
     * @return entity type string
     * @throws IllegalStateException when entity type class isn't mapped to any entity type
     */
    private synchronized static EntityType getEntityType(Class entityClass)
    {
        EntityType entityType = entityTypeByClass.get(entityClass);
        if (entityType == null) {
            for (Map.Entry<Class, EntityType> entry : entityTypeByClass.entrySet()) {
                Class entryClass = entry.getKey();
                if (entryClass.isAssignableFrom(entityClass)) {
                    entityType = entry.getValue();
                }
            }
            if (entityType == null) {
                throw new IllegalStateException(
                        String.format("Unknown identifier type for entity '%s'", entityClass.getName()));
            }
            entityTypeByClass.put(entityClass, entityType);
        }
        return entityType;
    }

    /**
     * @param entityType for which the class should be returned
     * @return entity class for given {@code entityType}
     */
    private static Class<? extends PersistentObject> getEntityTypeClass(EntityType entityType)
    {
        switch ( entityType ) {
            case RESOURCE:
                return Resource.class;
            case RESERVATION_REQUEST:
                return AbstractReservationRequest.class;
            case RESERVATION:
                return Reservation.class;
            case EXECUTABLE:
                return Executable.class;
            default:
                throw new TodoImplementException(entityType.toString());
        }
    }

    /**
     * @param entityClass to be checked
     * @return true if given {@code entityClass} is bound to any entity type,
     *         false otherwise
     */
    public static boolean hasEntityType(Class entityClass)
    {
        try {
            getEntityType(entityClass);
            return true;
        }
        catch (IllegalStateException exception) {
            return false;
        }
    }

    /**
     * @param entityId entity local id for the identifier
     * @return parsed {@link cz.cesnet.shongo.controller.common.EntityIdentifier}
     */
    public static EntityIdentifier parse(String entityId)
    {
        return parse(Domain.getLocalDomainName(), entityId);
    }

    /**
     * @param entityClass   entity type for the identifier
     * @param entityLocalId entity local id for the identifier
     * @return entity global identifier
     */
    public static String formatId(Class entityClass, Long entityLocalId)
    {
        return formatId(Domain.getLocalDomainName(), entityClass, entityLocalId);
    }

    /**
     * @param entityClass   entity type for the identifier
     * @param entityLocalId entity local id for the identifier
     * @return entity global identifier
     */
    public static String formatId(Class entityClass, String entityLocalId)
    {
        return formatId(Domain.getLocalDomainName(), entityClass, entityLocalId);
    }

    /**
     * @param resource for which the global identifier should be formatted
     * @return given {@code resource} global identifier.
     */
    public static String formatId(Resource resource)
    {
        resource.checkPersisted();
        return formatId(Domain.getLocalDomainName(), Resource.class, resource.getId());
    }

    /**
     * @param reservationRequest for which the global identifier should be formatted
     * @return given {@code resource} global identifier.
     */
    public static String formatId(AbstractReservationRequest reservationRequest)
    {
        reservationRequest.checkPersisted();
        return formatId(Domain.getLocalDomainName(), AbstractReservationRequest.class,
                reservationRequest.getId());
    }

    /**
     * @param reservation for which the global identifier should be formatted
     * @return given {@code resource} global identifier.
     */
    public static String formatId(Reservation reservation)
    {
        reservation.checkPersisted();
        return formatId(Domain.getLocalDomainName(), Reservation.class, reservation.getId());
    }

    /**
     * @param executable for which the global identifier should be formatted
     * @return given {@code resource} global identifier.
     */
    public static String formatId(Executable executable)
    {
        executable.checkPersisted();
        return formatId(Domain.getLocalDomainName(), Executable.class, executable.getId());
    }

    /**
     * @param entityClass entity type for the identifier
     * @param entityId    entity local id for the identifier
     * @return parsed local identifier from given global or local identifier
     */
    public static Long parseId(Class entityClass, String entityId)
    {
        return parseId(Domain.getLocalDomainName(), entityClass, entityId);
    }

    /**
     * Local identifier pattern.
     */
    private static Pattern LOCAL_IDENTIFIER_PATTERN = Pattern.compile("\\d+");

    /**
     * Global identifier pattern.
     */
    private static Pattern GLOBAL_IDENTIFIER_PATTERN = Pattern.compile("shongo:(.+):([a-z]+):(\\d+)");

    /**
     * @param domain   required domain
     * @param entityId
     * @return {@link cz.cesnet.shongo.controller.common.EntityIdentifier} parsed from given {@code entityId}
     */
    private static EntityIdentifier parse(String domain, String entityId)
    {
        Matcher matcher = GLOBAL_IDENTIFIER_PATTERN.matcher(entityId);
        if (!matcher.matches()) {
            throw new IdentifierWrongFormatException(entityId);
        }
        if (!domain.equals(matcher.group(1))) {
            throw new IdentifierWrongDomainException(entityId, domain);
        }
        EntityType entityType = entityTypeByCode.get(matcher.group(2));
        return new EntityIdentifier(entityType, Long.parseLong(matcher.group(3)));
    }

    /**
     * @param domain      domain name for the identifier
     * @param entityClass entity type for the identifier
     * @param entityId    entity local id for the identifier
     * @return parsed local identifier from given global or local identifier
     */
    private static Long parseId(String domain, Class entityClass, String entityId)
    {
        if (LOCAL_IDENTIFIER_PATTERN.matcher(entityId).matches()) {
            return Long.parseLong(entityId);
        }
        EntityIdentifier localIdentifier = parse(domain, entityId);
        EntityType requiredType = getEntityType(entityClass);
        if (localIdentifier.entityType != requiredType) {
            throw new IdentifierWrongTypeException(entityId, requiredType.getCode());
        }
        return localIdentifier.persistenceId;
    }

    /**
     * @param domain        domain name for the identifier
     * @param entityClass   entity type for the identifier
     * @param entityLocalId entity local id for the identifier
     * @return entity global identifier.
     */
    private static String formatId(String domain, Class entityClass, Long entityLocalId)
    {
        return formatId(domain, entityClass, entityLocalId.toString());
    }

    /**
     * @param domain        domain name for the identifier
     * @param entityClass   entity type for the identifier
     * @param entityLocalId entity local id for the identifier
     * @return entity global identifier.
     */
    private static String formatId(String domain, Class entityClass, String entityLocalId)
    {
        return String.format("shongo:%s:%s:%s", domain, getEntityType(entityClass).getCode(), entityLocalId);
    }
}

