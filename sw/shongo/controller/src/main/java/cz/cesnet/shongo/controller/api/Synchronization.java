package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import cz.cesnet.shongo.controller.ControllerReportSetHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Synchronization
{
    public static abstract class Handler<T extends PersistentObject, A>
    {
        private Class<T> objectClass;

        public Handler(Class<T> objectClass)
        {
            this.objectClass = objectClass;
        }

        public Class<T> getObjectClass()
        {
            return objectClass;
        }

        public void addToCollection(Collection<T> objects, T object)
        {
            objects.add(object);
        }

        public abstract T createFromApi(A objectApi);

        public abstract void updateFromApi(T object, A objectApi);
    }

    public static <T> void synchronizeCollection(Collection<T> objects, Collection<T> apiObjects)
    {
        objects.clear();
        objects.addAll(apiObjects);
    }

    public static <T extends PersistentObject, A extends IdentifiedComplexType>
    void synchronizeCollection(Collection<T> objects, Collection<A> apiObjects, Handler<T, A> handler)
    {
        Map<Long, T> existingObjects = new HashMap<Long, T>();
        for (T existingObject : objects) {
            existingObjects.put(existingObject.getId(), existingObject);
        }
        objects.clear();
        for (A apiObject : apiObjects) {
            T object = null;
            if (apiObject.hasId()) {
                Long objectId = apiObject.notNullIdAsLong();
                object = existingObjects.get(objectId);
                if (object == null) {
                    ControllerReportSetHelper.throwEntityNotFoundFault(handler.getObjectClass(), objectId);
                    return;
                }
                handler.updateFromApi(object, apiObject);
            }
            if (object == null) {
                object = handler.createFromApi(apiObject);
            }
            handler.addToCollection(objects, object);
        }
    }

    public static <T extends PersistentObject>
    void synchronizeCollectionPartial(Collection<T> objects, Collection<Object> apiObjects, Handler<T, Object> handler)
    {
        Map<Long, T> existingObjects = new HashMap<Long, T>();
        for (T existingObject : objects) {
            existingObjects.put(existingObject.getId(), existingObject);
        }
        objects.clear();
        for (Object apiObject : apiObjects) {
            T object = null;
            if (apiObject instanceof IdentifiedComplexType) {
                IdentifiedComplexType identifiedObjectApi = (IdentifiedComplexType) apiObject;
                if (identifiedObjectApi.hasId()) {
                    Long objectId = identifiedObjectApi.notNullIdAsLong();
                    object = existingObjects.get(objectId);
                    if (object == null) {
                        ControllerReportSetHelper.throwEntityNotFoundFault(handler.getObjectClass(), objectId);
                        return;
                    }
                    handler.updateFromApi(object, apiObject);
                }
            }
            if (object == null) {
                object = handler.createFromApi(apiObject);
            }
            handler.addToCollection(objects, object);
        }
    }
}
