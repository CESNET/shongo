package cz.cesnet.shongo.controller.booking;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.acl.AclObjectClass;
import cz.cesnet.shongo.controller.acl.AclObjectIdentity;
import cz.cesnet.shongo.controller.booking.domain.Domain;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.ForeignResources;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.resource.Tag;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolver for {@link ObjectType}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ObjectTypeResolver
{
    /**
     * Object types by class.
     */
    private static final Map<Class<? extends PersistentObject>, ObjectType> objectTypeByClass =
            new HashMap<Class<? extends PersistentObject>, ObjectType>();

    /**
     * Static initialization.
     */
    static {
        for (ObjectType objectType : ObjectType.class.getEnumConstants()) {
            objectTypeByClass.put(getObjectTypeClass(objectType), objectType);
        }
    }

    /**
     * @param objectClass object type class
     * @return object type string
     * @throws RuntimeException when object type class isn't mapped to any object type
     */
    public synchronized static ObjectType getObjectType(Class<? extends PersistentObject> objectClass)
    {
        ObjectType objectType = objectTypeByClass.get(objectClass);
        if (objectType == null) {
            for (Map.Entry<Class<? extends PersistentObject>, ObjectType> entry : objectTypeByClass.entrySet()) {
                Class<? extends PersistentObject> entryClass = entry.getKey();
                if (entryClass.isAssignableFrom(objectClass)) {
                    objectType = entry.getValue();
                }
            }
            if (objectType == null) {
                throw new RuntimeException(
                        String.format("Unknown identifier type for object '%s'", objectClass.getName()));
            }
            objectTypeByClass.put(objectClass, objectType);
        }
        return objectType;
    }

    /**
     * @param object
     * @return {@link ObjectType} for given {@code object}
     */
    public static ObjectType getObjectType(PersistentObject object)
    {
        return getObjectType(object.getClass());
    }

    /**
     * @param objectIdentity
     * @return {@link ObjectType} for given {@code objectIdentity}
     */
    public static ObjectType getObjectType(AclObjectIdentity objectIdentity)
    {
        return getObjectType(objectIdentity.getObjectClass());
    }

    /**
     * @param aclObjectClass
     * @return {@link ObjectType} for given {@code aclObjectClass}
     */
    public static ObjectType getObjectType(AclObjectClass aclObjectClass)
    {
        return ObjectType.valueOf(aclObjectClass.getClassName());
    }

    /**
     * @param objectType for which the class should be returned
     * @return object class for given {@code objectType}
     */
    public static Class<? extends PersistentObject> getObjectTypeClass(ObjectType objectType)
    {
        switch (objectType) {
            case RESOURCE:
                return Resource.class;
            case RESERVATION_REQUEST:
                return AbstractReservationRequest.class;
            case RESERVATION:
                return Reservation.class;
            case EXECUTABLE:
                return Executable.class;
            case TAG:
                return Tag.class;
            case DOMAIN:
                return Domain.class;
            case FOREIGN_RESOURCES:
                return ForeignResources.class;
            default:
                throw new TodoImplementException(objectType);
        }
    }

    /**
     * @param objectIdentity for which the class should be returned
     * @return object class for given {@code objectIdentity}
     */
    public static Class<? extends PersistentObject> getObjectTypeClass(AclObjectIdentity objectIdentity)
    {
        return getObjectTypeClass(objectIdentity.getObjectClass());
    }

    /**
     * @param objectClass for which the class should be returned
     * @return object class for given {@code objectClass}
     */
    public static Class<? extends PersistentObject> getObjectTypeClass(AclObjectClass objectClass)
    {
        return getObjectTypeClass(getObjectType(objectClass));
    }
}
