package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.fault.IdentifierWrongDomainException;
import cz.cesnet.shongo.controller.fault.IdentifierWrongTypeException;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.fault.FaultException;

import java.util.HashMap;
import java.util.Map;
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
     * Entity types by class.
     */
    private static final Map<Class, String> entityTypeByClass = new HashMap<Class, String>()
    {{
            put(Resource.class, "res");
            put(AbstractReservationRequest.class, "req");
            put(Reservation.class, "rsv");
            put(Executable.class, "exe");
        }};

    /**
     * @param entityClass entity type class
     * @return entity type string
     * @throws IllegalStateException when entity type class isn't mapped to any entity type
     */
    private synchronized static String getEntityType(Class entityClass)
    {
        String entityType = entityTypeByClass.get(entityClass);
        if (entityType == null) {
            for (Map.Entry<Class, String> entry : entityTypeByClass.entrySet()) {
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
        return String.format("shongo:%s:%s:%d", domain, getEntityType(entityClass), entityLocalId);
    }

    /**
     * @param domain        domain name for the identifier
     * @param entityClass   entity type for the identifier
     * @param entityLocalId entity local id for the identifier
     * @return entity global identifier.
     */
    private static String formatGlobalId(String domain, Class entityClass, String entityLocalId)
    {
        return String.format("shongo:%s:%s:%s", domain, getEntityType(entityClass), entityLocalId);
    }

    /**
     * @param domain      domain name for the identifier
     * @param entityClass entity type for the identifier
     * @param entityId    entity local id for the identifier
     * @return parsed local identifier from given global or local identifier
     */
    private static Long parseLocalId(String domain, Class entityClass, String entityId)
    {
        if (Pattern.matches("\\d+", entityId)) {
            return Long.parseLong(entityId);
        }
        String domainPrefix = String.format("shongo:%s:", domain);
        if (!entityId.startsWith(domainPrefix)) {
            throw new IdentifierWrongDomainException(entityId, domain);
        }
        String requiredType = getEntityType(entityClass);
        String domainTypePrefix = String.format("%s%s:", domainPrefix, requiredType);
        if (!entityId.startsWith(domainTypePrefix)) {
            throw new IdentifierWrongTypeException(entityId, requiredType);
        }
        return Long.parseLong(entityId.substring(domainTypePrefix.length(), entityId.length()));
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
}
