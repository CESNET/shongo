package cz.cesnet.shongo.map;

/**
 * Base class for all API objects which can be serialized to/from {@link java.util.Map}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractObject
{
    public static final String CLASS_PROPERTY = "new_class";

    /**
     * @return name of the class
     */
    protected String getClassName()
    {
        String className = getClass().getName();
        String[] parts = className.split("\\.");
        return parts[parts.length - 1];
    }

    /**
     * @return this object serialized to {@link DataMap}
     */
    public DataMap toData()
    {
        DataMap data = new DataMap();
        data.set(CLASS_PROPERTY, getClassName());
        return data;
    }

    /**
     * @param data from which this object should be de-serialized
     */
    public void fromData(DataMap data)
    {
        String className = getClassName();
        String newClassName = data.getString(CLASS_PROPERTY);
        if (!newClassName.equals(className)) {
            throw new IllegalArgumentException("Invalid class " + newClassName + ". " + className + " is required.");
        }
    }
}
