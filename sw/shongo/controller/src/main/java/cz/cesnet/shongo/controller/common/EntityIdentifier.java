package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.EntityType;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.Resource;

import javax.persistence.*;
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
@Embeddable
public class EntityIdentifier
{
    /**
     * {@link EntityType} of the identifier.
     */
    private EntityType entityType;

    /**
     * Identifier value.
     */
    private Long persistenceId;

    /**
     * Constructor.
     */
    public EntityIdentifier()
    {
        this.entityType = null;
        this.persistenceId = null;
    }

    /**
     * Constructor.
     *
     * @param entityType sets the {@link #entityType}
     */
    public EntityIdentifier(EntityType entityType)
    {
        this.entityType = entityType;
        this.persistenceId = null;
    }

    /**
     * Constructor.
     *
     * @param entityType    sets the {@link #entityType}
     * @param persistenceId sets the {@link #persistenceId}
     */
    public EntityIdentifier(EntityType entityType, Long persistenceId)
    {
        this.entityType = entityType;
        this.persistenceId = persistenceId;
    }

    /**
     * Constructor.
     *
     * @param persistentObject sets the {@link #entityType} and the {@link #persistenceId}
     */
    public EntityIdentifier(PersistentObject persistentObject)
    {
        this.entityType = getEntityType(persistentObject.getClass());
        this.persistenceId = persistentObject.getId();
    }

    /**
     * @return {@link #entityType}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public EntityType getEntityType()
    {
        return entityType;
    }

    /**
     * @param entityType sets the {@link #entityType}
     */
    public void setEntityType(EntityType entityType)
    {
        this.entityType = entityType;
    }

    /**
     * @return true whether {@link #entityType} is empty or {@link #persistenceId} is empty,
     *         false otherwise
     */
    @Transient
    public boolean isGroup()
    {
        return entityType == null || persistenceId == null;
    }

    /**
     * @return {@link #persistenceId}
     */
    @Column
    public Long getPersistenceId()
    {
        return persistenceId;
    }

    /**
     * @param persistenceId sets the {@link #persistenceId}
     */
    public void setPersistenceId(Long persistenceId)
    {
        this.persistenceId = persistenceId;
    }

    /**
     * @return class for the {@link #entityType}
     */
    @Transient
    public Class<? extends PersistentObject> getEntityClass()
    {
        return getEntityTypeClass(entityType);
    }

    /**
     * @return global identifier
     */
    public String toId()
    {
        return formatId(entityType, persistenceId);
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        EntityIdentifier that = (EntityIdentifier) object;

        if (entityType != that.entityType) {
            return false;
        }
        if (persistenceId != null ? !persistenceId.equals(that.persistenceId) : that.persistenceId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = entityType != null ? entityType.hashCode() : 0;
        result = 31 * result + (persistenceId != null ? persistenceId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return toId();
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
     * @throws RuntimeException when entity type class isn't mapped to any entity type
     */
    public synchronized static EntityType getEntityType(Class entityClass)
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
                throw new RuntimeException(
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
        switch (entityType) {
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
        catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * @param entityId entity identifier
     * @return parsed {@link cz.cesnet.shongo.controller.common.EntityIdentifier}
     */
    public static EntityIdentifier parse(String entityId)
    {
        if (entityId == null) {
            return null;
        }
        Matcher matcher = LOCAL_TYPE_IDENTIFIER_PATTERN.matcher(entityId);
        if (matcher.matches()) {
            return new EntityIdentifier(entityTypeByCode.get(matcher.group(1)), parsePersistenceId(matcher.group(2)));
        }
        return parse(Domain.getLocalDomainName(), entityId);
    }

    /**
     * @param entityId   entity identifier
     * @param entityType
     * @return parsed {@link cz.cesnet.shongo.controller.common.EntityIdentifier}
     */
    public static EntityIdentifier parse(String entityId, EntityType entityType)
    {
        return parse(Domain.getLocalDomainName(), entityType, entityId);
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
     * @param entityType    entity type for the identifier
     * @param entityLocalId entity local id for the identifier
     * @return entity global identifier
     */
    public static String formatId(EntityType entityType, Long entityLocalId)
    {
        return formatId(Domain.getLocalDomainName(), entityType, entityLocalId);
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
     * @param entity for which the global identifier should be formatted
     * @return given {@code entity} global identifier.
     */
    public static String formatId(PersistentObject entity)
    {
        return formatId(entity.getClass(), entity.getId());
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
    private static Pattern LOCAL_IDENTIFIER_PATTERN = Pattern.compile("\\d+|\\*");

    /**
     * Local identifier pattern with type.
     */
    private static Pattern LOCAL_TYPE_IDENTIFIER_PATTERN = Pattern.compile("([a-z]+|\\*):(\\d+|\\*)");

    /**
     * Global identifier pattern.
     */
    private static Pattern GLOBAL_IDENTIFIER_PATTERN = Pattern.compile("shongo:(.+):([a-z]+|\\*):(\\d+|\\*)");

    /**
     * @param persistenceId to be parsed
     * @return parsed persistence id
     */
    private static Long parsePersistenceId(String persistenceId)
    {
        if (persistenceId.equals("*")) {
            return null;
        }
        return Long.parseLong(persistenceId);
    }

    /**
     * @param domain   required domain
     * @param entityId
     * @return {@link cz.cesnet.shongo.controller.common.EntityIdentifier} parsed from given {@code entityId}
     */
    private static EntityIdentifier parse(String domain, String entityId)
            throws ControllerReportSet.IdentifierInvalidException,
                   ControllerReportSet.IdentifierInvalidDomainException
    {
        Matcher matcher = GLOBAL_IDENTIFIER_PATTERN.matcher(entityId);
        if (!matcher.matches()) {
            throw new ControllerReportSet.IdentifierInvalidException(entityId);
        }
        if (!domain.equals(matcher.group(1))) {
            throw new ControllerReportSet.IdentifierInvalidDomainException(entityId, domain);
        }
        EntityType entityType = entityTypeByCode.get(matcher.group(2));
        return new EntityIdentifier(entityType, parsePersistenceId(matcher.group(3)));
    }

    /**
     * @param domain     required domain
     * @param entityType
     * @param entityId
     * @return {@link cz.cesnet.shongo.controller.common.EntityIdentifier} parsed from given {@code entityId}
     */
    private static EntityIdentifier parse(String domain, EntityType entityType, String entityId)
            throws ControllerReportSet.IdentifierInvalidException,
                   ControllerReportSet.IdentifierInvalidDomainException,
                   ControllerReportSet.IdentifierInvalidTypeException
    {
        if (LOCAL_IDENTIFIER_PATTERN.matcher(entityId).matches()) {
            return new EntityIdentifier(entityType, parsePersistenceId(entityId));
        }
        EntityIdentifier entityIdentifier = parse(domain, entityId);
        if (entityIdentifier.entityType != entityType) {
            throw new ControllerReportSet.IdentifierInvalidTypeException(entityId, entityType.getCode());
        }
        return entityIdentifier;
    }

    /**
     * @param domain      domain name for the identifier
     * @param entityClass entity type for the identifier
     * @param entityId    entity local id for the identifier
     * @return parsed local identifier from given global or local identifier
     */
    private static Long parseId(String domain, Class entityClass, String entityId)
            throws ControllerReportSet.IdentifierInvalidException,
                   ControllerReportSet.IdentifierInvalidDomainException,
                   ControllerReportSet.IdentifierInvalidTypeException
    {
        if (LOCAL_IDENTIFIER_PATTERN.matcher(entityId).matches()) {
            return parsePersistenceId(entityId);
        }
        EntityIdentifier entityIdentifier = parse(domain, entityId);
        EntityType requiredType = getEntityType(entityClass);
        if (entityIdentifier.entityType != requiredType) {
            throw new ControllerReportSet.IdentifierInvalidTypeException(entityId, requiredType.getCode());
        }
        return entityIdentifier.persistenceId;
    }

    /**
     * @param domain        domain name for the identifier
     * @param entityClass   entity type for the identifier
     * @param entityLocalId entity local id for the identifier
     * @return entity global identifier.
     */
    private static String formatId(String domain, Class entityClass, Long entityLocalId)
    {
        return formatId(domain, entityClass, (entityLocalId == null ? null : entityLocalId.toString()));
    }

    /**
     * @param domain        domain name for the identifier
     * @param entityType    entity type for the identifier
     * @param entityLocalId entity local id for the identifier
     * @return entity global identifier.
     */
    private static String formatId(String domain, EntityType entityType, Long entityLocalId)
    {
        return formatId(domain, entityType, (entityLocalId == null ? null : entityLocalId.toString()));
    }

    /**
     * @param domain        domain name for the identifier
     * @param entityClass   entity type for the identifier
     * @param entityLocalId entity local id for the identifier
     * @return entity global identifier.
     */
    private static String formatId(String domain, Class entityClass, String entityLocalId)
    {
        return formatId(domain, getEntityType(entityClass), entityLocalId);
    }

    /**
     * @param domain        domain name for the identifier
     * @param entityType    entity type for the identifier
     * @param entityLocalId entity local id for the identifier
     * @return entity global identifier.
     */
    private static String formatId(String domain, EntityType entityType, String entityLocalId)
    {
        return String.format("shongo:%s:%s:%s", domain,
                (entityType == null ? "*" : entityType.getCode()),
                (entityLocalId == null ? "*" : entityLocalId));
    }
}

