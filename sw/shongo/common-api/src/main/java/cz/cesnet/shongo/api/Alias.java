package cz.cesnet.shongo.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.rpc.StructType;
import cz.cesnet.shongo.api.util.IdentifiedObject;
import jade.content.Concept;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Alias extends IdentifiedObject implements StructType, Concept
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
}
