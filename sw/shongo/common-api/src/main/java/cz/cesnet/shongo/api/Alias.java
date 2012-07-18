package cz.cesnet.shongo.api;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Alias
{
    /**
     * Technology of alias.
     */
    private Technology technology;

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
     *
     * @param technology    
     * @param type
     * @param value
     */
    public Alias(Technology technology, AliasType type, String value)
    {
        this.technology = technology;
        this.type = type;
        this.value = value;
    }

    public Technology getTechnology()
    {
        return technology;
    }

    public void setTechnology(Technology technology)
    {
        this.technology = technology;
    }

    public AliasType getType()
    {
        return type;
    }

    public void setType(AliasType type)
    {
        this.type = type;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }
}
