package cz.cesnet.shongo.controller.booking;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.EntityType;
import cz.cesnet.shongo.controller.acl.AclObjectClass;
import cz.cesnet.shongo.controller.acl.AclObjectIdentity;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.Resource;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class EntityTypeResolver
{
    /**
     * Entity types by class.
     */
    private static final Map<Class<? extends PersistentObject>, EntityType> entityTypeByClass =
            new HashMap<Class<? extends PersistentObject>, EntityType>();

    /**
     * Static initialization.
     */
    static {
        for (EntityType entityType : EntityType.class.getEnumConstants()) {
            entityTypeByClass.put(getEntityTypeClass(entityType), entityType);
        }
    }

    /**
     * @param entityClass entity type class
     * @return entity type string
     * @throws RuntimeException when entity type class isn't mapped to any entity type
     */
    public synchronized static EntityType getEntityType(Class<? extends PersistentObject> entityClass)
    {
        EntityType entityType = entityTypeByClass.get(entityClass);
        if (entityType == null) {
            for (Map.Entry<Class<? extends PersistentObject>, EntityType> entry : entityTypeByClass.entrySet()) {
                Class<? extends PersistentObject> entryClass = entry.getKey();
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
     * @param object
     * @return {@link EntityType} for given {@code object}
     */
    public static EntityType getEntityType(PersistentObject object)
    {
        return getEntityType(object.getClass());
    }

    /**
     * @param objectIdentity
     * @return {@link EntityType} for given {@code objectIdentity}
     */
    public static EntityType getEntityType(AclObjectIdentity objectIdentity)
    {
        return getEntityType(objectIdentity.getObjectClass());
    }

    /**
     * @param aclObjectClass
     * @return {@link EntityType} for given {@code aclObjectClass}
     */
    public static EntityType getEntityType(AclObjectClass aclObjectClass)
    {
        return EntityType.valueOf(aclObjectClass.getClassName());
    }

    /**
     * @param entityType for which the class should be returned
     * @return entity class for given {@code entityType}
     */
    public static Class<? extends PersistentObject> getEntityTypeClass(EntityType entityType)
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
                throw new TodoImplementException(entityType);
        }
    }

    /**
     * @param objectIdentity for which the class should be returned
     * @return object class for given {@code objectIdentity}
     */
    public static Class<? extends PersistentObject> getEntityTypeClass(AclObjectIdentity objectIdentity)
    {
        return getEntityTypeClass(objectIdentity.getObjectClass());
    }

    /**
     * @param objectClass for which the class should be returned
     * @return object class for given {@code objectClass}
     */
    public static Class<? extends PersistentObject> getEntityTypeClass(AclObjectClass objectClass)
    {
        return getEntityTypeClass(getEntityType(objectClass));
    }
}
