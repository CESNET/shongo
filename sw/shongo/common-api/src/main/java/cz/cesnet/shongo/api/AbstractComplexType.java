package cz.cesnet.shongo.api;

/**
 * Represents object which can be serialized to/from {@link java.util.Map}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractComplexType implements ComplexType
{
    @Override
    public String getClassName()
    {
        return ClassHelper.getClassShortName(getClass());
    }

    @Override
    public DataMap toData()
    {
        DataMap data = new DataMap(this);
        data.set(CLASS_PROPERTY, getClassName());
        return data;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        String className = getClassName();
        String newClassName = dataMap.getString(CLASS_PROPERTY);
        if (!newClassName.equals(className)) {
            throw new IllegalArgumentException("Invalid class " + newClassName + ". " + className + " is required.");
        }
    }
}
