package cz.cesnet.shongo.common.util;

import cz.cesnet.shongo.common.xmlrpc.Fault;
import cz.cesnet.shongo.common.xmlrpc.FaultException;

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
     * @throws cz.cesnet.shongo.common.xmlrpc.FaultException
     *          is thrown when the described condition is not met
     */
    public void checkRequired(String name) throws FaultException
    {
        if (map.containsKey(name) && map.get(name) != null) {
            return;
        }
        throw new FaultException(Fault.Common.ATTRIBUTE_IS_REQUIRED, name, getEntityClassName());
    }

    /**
     * @param name
     * @param enumClass
     * @return enum value from attribute with given name for specified enum class
     * @throws FaultException
     */
    public <T extends Enum<T>> T getEnum(String name, Class<T> enumClass) throws FaultException
    {
        Object value = map.get(name);
        if (value instanceof String) {
            return Converter.convertStringToEnum((String) value, enumClass);
        }
        else {
            throw new FaultException(Fault.Common.ATTRIBUTE_TYPE_MISMATCH, name, getEntityClassName(),
                    Converter.getShortClassName(String.class), Converter.getShortClassName(value.getClass()));
        }
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
}
