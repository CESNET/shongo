package cz.cesnet.shongo.api;

/**
 * Represents object that can be serialized to/from {@link java.util.Map}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ComplexType
{
    public static final String CLASS_PROPERTY = "class";

    /**
     * @return name of the {@link ComplexType} class
     */
    public String getClassName();

    /**
     * @return the {@link ComplexType} serialized to {@link DataMap}
     */
    public DataMap toData();

    /**
     * @param dataMap from which the {@link ComplexType} should be de-serialized
     */
    public void fromData(DataMap dataMap);
}
