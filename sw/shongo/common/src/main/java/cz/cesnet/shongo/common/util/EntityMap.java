package cz.cesnet.shongo.common.util;

import cz.cesnet.shongo.common.api.Fault;
import cz.cesnet.shongo.common.api.FaultException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Helper class for manipulating with map that contains attributes for an entity.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class EntityMap
{
    /**
     * Map that contains values for the entity.
     */
    private Map map;

    /**
     * Name of entity class.
     */
    private String entityClassName;

    /**
     * Constructor.
     *
     * @param map
     * @param entityClassName
     */
    public EntityMap(Map map, String entityClassName)
    {
        this.map = map;
        this.entityClassName = entityClassName;
    }

    /**
     * Constructor.
     *
     * @param map
     * @param entityClass
     */
    public EntityMap(Map map, Class entityClass)
    {
        this(map, Converter.getShortClassName(entityClass));
    }

    /**
     * @return {@link #entityClassName}
     */
    public String getEntityClassName()
    {
        return entityClassName;
    }

    /**
     * Checks whether attribute with given name is present in map and it's value is not null.
     *
     * @param name
     * @throws cz.cesnet.shongo.common.api.FaultException
     *          is thrown when the described condition is not met
     */
    public void checkRequired(String name) throws FaultException
    {
        if (map.containsKey(name) && map.get(name) != null) {
            return;
        }
        throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_IS_REQUIRED, name, getEntityClassName());
    }

    /**
     * @param name
     * @param valueClass
     * @return value from attribute with given name
     * @throws FaultException when attribute type doesn't match the given class
     */
    public <T> T getAttribute(String name, Class<T> valueClass) throws FaultException
    {
        Object value = map.get(name);
        if (valueClass.isAssignableFrom(value.getClass())) {
            return valueClass.cast(value);
        }
        else {
            throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_TYPE_MISMATCH, name, getEntityClassName(),
                    Converter.getShortClassName(valueClass), Converter.getShortClassName(value.getClass()));
        }
    }

    public Object getAttribute(String name, Class[] valueClasses) throws FaultException
    {
        Object value = map.get(name);
        for ( Class valueClass : valueClasses) {
            if (valueClass.isAssignableFrom(value.getClass())) {
                return valueClass.cast(value);
            }
        }
        StringBuilder types = new StringBuilder();
        for ( Class valueClass : valueClasses) {
            if (types.length() > 0 ) {
                types.append("|");
            }
            types.append(Converter.getShortClassName(valueClass));
        }
        throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_TYPE_MISMATCH, name, getEntityClassName(),
                    types.toString(), Converter.getShortClassName(value.getClass()));
    }

    /**
     * @param name
     * @param enumClass
     * @return enum value from attribute with given name for specified enum class
     * @throws FaultException
     */
    public <T extends Enum<T>> T getEnum(String name, Class<T> enumClass) throws FaultException
    {
        String value = getAttribute(name, String.class);
        return Converter.stringToEnum(value, enumClass);
    }

    /**
     * @param name
     * @param enumClass
     * @return enum value from attribute with given name for specified enum class
     * @throws FaultException
     */
    public <T extends Enum<T>> T getEnumRequired(String name, Class<T> enumClass) throws FaultException
    {
        checkRequired(name);
        return getEnum(name, enumClass);
    }

    /**
     * @param name
     * @param itemClass
     * @return collection of items of specified type which are present in array attribute with given name
     * @throws FaultException
     */
    public <T> Collection<T> getCollection(String name, Class<T> itemClass) throws FaultException
    {
        Object[] value = getAttribute(name, Object[].class);
        List<T> list = new ArrayList<T>();
        for (Object item : value) {
            if (itemClass.isAssignableFrom(item.getClass())) {
                list.add((T) item);
            }
            else {
                throw new FaultException(Fault.Common.COLLECTION_ITEM_TYPE_MISMATCH, name,
                        Converter.getShortClassName(itemClass), Converter.getShortClassName(value.getClass()));
            }
        }
        return list;
    }

    /**
     * @param name
     * @param itemClass
     * @return collection of items of specified type which are present in array attribute with given name
     * @throws FaultException
     */
    public <T> Collection<T> getCollectionRequired(String name, Class<T> itemClass) throws FaultException
    {
        checkRequired(name);
        return getCollection(name, itemClass);
    }
}
