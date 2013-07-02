package cz.cesnet.shongo.api;

import cz.cesnet.shongo.AliasType;
import jade.content.Concept;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Alias extends IdentifiedComplexType
{
    /**
     * Type of alias.
     */
    private AliasType type;

    /**
     * Value of alias.
     */
    private String value;

    /**
     * Constructor.
     */
    public Alias()
    {
    }

    /**
     * Constructor.
     *
     * @param type
     * @param value
     */
    public Alias(AliasType type, String value)
    {
        this.type = type;
        this.value = value;
    }

    /**
     * @return {@link #type}
     */
    public AliasType getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(AliasType type)
    {
        this.type = type;
    }

    /**
     * @return {@link #value}
     */
    public String getValue()
    {
        return value;
    }

    /**
     * @param value sets the {@link #value}
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        return String.format(Alias.class.getSimpleName() + " (type: %s, value: %s)", type.toString(), value);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Alias alias = (Alias) o;

        if (type != alias.type) {
            return false;
        }
        if (!value.equals(alias.value)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = type.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    private static final String TYPE = "type";
    private static final String VALUE = "value";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(TYPE, type);
        dataMap.set(VALUE, value);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        type = dataMap.getEnum(TYPE, AliasType.class);
        value = dataMap.getString(VALUE);
    }
}
