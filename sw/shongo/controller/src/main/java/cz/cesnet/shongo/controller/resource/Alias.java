package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.common.PersistentObject;

import javax.persistence.*;

/**
 * Represents an alias for a single device and a single technology.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class Alias extends PersistentObject
{
    /**
     * Enumeration for alias type.
     */
    public static enum Type
    {
        /**
         * @see <a href="http://en.wikipedia.org/wiki/E.164">E.164</a>
         */
        E164,

        /**
         * e.g., H.323 ID
         */
        Identifier,

        /**
         * e.g., H.323 or SIP URI
         */
        URI
    }

    /**
     * Technology of alias.
     */
    private Technology technology;

    /**
     * Type of alias.
     */
    private Type type;

    /**
     * Value of alias.
     */
    private String value;

    /**
     * Constructor.
     * @param technology
     * @param type
     * @param value
     */
    public Alias(Technology technology, Type type, String value)
    {
        this.technology = technology;
        this.type = type;
        this.value = value;
    }

    /**
     * @return {@link #technology}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public Technology getTechnology()
    {
        return technology;
    }

    /**
     * @param technology sets the {@link #technology}
     */
    private void setTechnology(Technology technology)
    {
        this.technology = technology;
    }

    /**
     * @return {@link #type}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public Type getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    private void setType(Type type)
    {
        this.type = type;
    }

    /**
     * @return {@link #value}
     */
    @Column
    public String getValue()
    {
        return value;
    }

    /**
     * @param value sets the {@link #value}
     */
    private void setValue(String value)
    {
        this.value = value;
    }
}
