package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.fault.IdentifierWrongDomainException;
import cz.cesnet.shongo.controller.fault.IdentifierWrongFormatException;
import cz.cesnet.shongo.controller.fault.IdentifierWrongTypeException;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.Resource;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a helper class for formatting/parsing Shongo identifiers.
 * <p/>
 * Identifiers are of following format:
 * <p/>
 * {@code shongo:<domain>:<entity-type>:<entity-id>}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class IdentifierFormat
{
    /**
     * Shongo identifier entity type.
     */
    public static enum EntityType
    {
        RESOURCE("res", Resource.class),
        RESERVATION_REQUEST("req", AbstractReservationRequest.class),
        RESERVATION("rsv", Reservation.class),
        EXECUTABLE("exe", Executable.class);

        /**
         * Code of the {@link EntityType}.
         */
        private String code;

        /**
         * Class of the {@link EntityType}.
         */
        private Class type;

        /**
         * @param code sets the {@link #code}
         */
        private EntityType(String code, Class type)
        {
            this.code = code;
            this.type = type;
        }

        /**
         * @return {@link #code}
         */
        public String getCode()
        {
            return code;
        }

        /**
         * @return {@link #type}
         */
        public Class getType()
        {
            return type;
        }
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
            entityTypeByClass.put(entityType.getType(), entityType);
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
     * @param domain        domain name for the identifier
     * @param entityClass   entity type for the identifier
     * @param entityLocalId entity local id for the identifier
     * @return entity global identifier.
     */
    private static String formatGlobalId(String domain, Class entityClass, Long entityLocalId)
    {
        return formatGlobalId(domain, entityClass, entityLocalId.toString());
    }

    /**
     * @param domain        domain name for the identifier
     * @param entityClass   entity type for the identifier
     * @param entityLocalId entity local id for the identifier
     * @return entity global identifier.
     */
    private static String formatGlobalId(String domain, Class entityClass, String entityLocalId)
    {
        return String.format("shongo:%s:%s:%s", domain, getEntityType(entityClass).getCode(), entityLocalId);
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
     * @return {@link LocalIdentifier} parsed from given {@code entityId}
     */
    private static LocalIdentifier parseLocalIdentifier(String domain, String entityId)
    {
        Matcher matcher = GLOBAL_IDENTIFIER_PATTERN.matcher(entityId);
        if (!matcher.matches()) {
            throw new IdentifierWrongFormatException(entityId);
        }
        if (!domain.equals(matcher.group(1))) {
            throw new IdentifierWrongDomainException(entityId, domain);
        }
        EntityType entityType = entityTypeByCode.get(matcher.group(2));
        return new LocalIdentifier(entityType, Long.parseLong(matcher.group(3)));
    }

    /**
     * @param domain      domain name for the identifier
     * @param entityClass entity type for the identifier
     * @param entityId    entity local id for the identifier
     * @return parsed local identifier from given global or local identifier
     */
    private static Long parseLocalId(String domain, Class entityClass, String entityId)
    {
        if (LOCAL_IDENTIFIER_PATTERN.matcher(entityId).matches()) {
            return Long.parseLong(entityId);
        }
        LocalIdentifier localIdentifier = parseLocalIdentifier(domain, entityId);
        EntityType requiredType = getEntityType(entityClass);
        if (localIdentifier.entityType != requiredType) {
            throw new IdentifierWrongTypeException(entityId, requiredType.getCode());
        }
        return localIdentifier.entityId;
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
     * @param entityClass   entity type for the identifier
     * @param entityLocalId entity local id for the identifier
     * @return entity global identifier
     */
    public static String formatGlobalId(Class entityClass, Long entityLocalId)
    {
        return formatGlobalId(Domain.getLocalDomainName(), entityClass, entityLocalId);
    }

    /**
     * @param entityClass   entity type for the identifier
     * @param entityLocalId entity local id for the identifier
     * @return entity global identifier
     */
    public static String formatGlobalId(Class entityClass, String entityLocalId)
    {
        return formatGlobalId(Domain.getLocalDomainName(), entityClass, entityLocalId);
    }

    /**
     * @param resource for which the global identifier should be formatted
     * @return given {@code resource} global identifier.
     */
    public static String formatGlobalId(Resource resource)
    {
        resource.checkPersisted();
        return formatGlobalId(Domain.getLocalDomainName(), Resource.class, resource.getId());
    }

    /**
     * @param reservationRequest for which the global identifier should be formatted
     * @return given {@code resource} global identifier.
     */
    public static String formatGlobalId(AbstractReservationRequest reservationRequest)
    {
        reservationRequest.checkPersisted();
        return formatGlobalId(Domain.getLocalDomainName(), AbstractReservationRequest.class,
                reservationRequest.getId());
    }

    /**
     * @param reservation for which the global identifier should be formatted
     * @return given {@code resource} global identifier.
     */
    public static String formatGlobalId(Reservation reservation)
    {
        reservation.checkPersisted();
        return formatGlobalId(Domain.getLocalDomainName(), Reservation.class, reservation.getId());
    }

    /**
     * @param executable for which the global identifier should be formatted
     * @return given {@code resource} global identifier.
     */
    public static String formatGlobalId(Executable executable)
    {
        executable.checkPersisted();
        return formatGlobalId(Domain.getLocalDomainName(), Executable.class, executable.getId());
    }

    /**
     * @param entityClass entity type for the identifier
     * @param entityId    entity local id for the identifier
     * @return parsed local identifier from given global or local identifier
     */
    public static Long parseLocalId(Class entityClass, String entityId)
    {
        return parseLocalId(Domain.getLocalDomainName(), entityClass, entityId);
    }

    /**
     * @param entityId entity local id for the identifier
     * @return parsed {@link LocalIdentifier}
     */
    public static LocalIdentifier parseLocalId(String entityId)
    {
        return parseLocalIdentifier(Domain.getLocalDomainName(), entityId);
    }

    /**
     * Represents a local identifier.
     */
    public static class LocalIdentifier
    {
        /**
         * {@link EntityType} of the identifier.
         */
        private final EntityType entityType;

        /**
         * Identifier value.
         */
        private final Long entityId;

        /**
         * Constructor.
         *
         * @param entityType sets the {@link #entityType}
         * @param entityId sets the {@link #entityId}
         */
        public LocalIdentifier(EntityType entityType, Long entityId)
        {
            this.entityType = entityType;
            this.entityId = entityId;
        }

        /**
         * @return {@link #entityType}
         */
        public EntityType getEntityType()
        {
            return entityType;
        }

        /**
         * @return {@link #entityId}
         */
        public Long getEntityId()
        {
            return entityId;
        }
    }
}
