package cz.cesnet.shongo.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.xmlrpc.StructType;
import jade.content.Concept;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Alias implements StructType, Concept
{
    /**
     * Identifier.
     */
    private Integer id;

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
     */
    public Alias()
    {
    }

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

    /**
     * @return {@link #id
     */
    public Integer getId()
    {
        return id;
    }

    /**
     * @param id sets the {@link #id}
     */
    public void setId(Integer id)
    {
        this.id = id;
    }

    /**
     * @return {@link #technology}
     */
    public Technology getTechnology()
    {
        return technology;
    }

    /**
     * @param technology sets the {@link #technology}
     */
    public void setTechnology(Technology technology)
    {
        this.technology = technology;
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
}
