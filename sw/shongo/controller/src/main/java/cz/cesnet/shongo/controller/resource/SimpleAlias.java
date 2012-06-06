package cz.cesnet.shongo.controller.resource;

/**
 * Represents an alias that is defined by all alias attributes.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public final class SimpleAlias implements Alias
{
    /**
     * Technology of alias.
     */
    private final Technology technology;

    /**
     * Type of alias.
     */
    private final Type type;

    /**
     * Value of alias.
     */
    private final String value;

    /**
     * Constructor.
     * @param technology
     * @param type
     * @param value
     */
    public SimpleAlias(Technology technology, Type type, String value)
    {
        this.technology = technology;
        this.type = type;
        this.value = value;
    }

    @Override
    public Technology getTechnology()
    {
        return technology;
    }

    @Override
    public Type getType()
    {
        return type;
    }

    @Override
    public String getValue()
    {
        return value;
    }
}
