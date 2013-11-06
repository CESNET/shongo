package cz.cesnet.shongo.controller.booking.alias;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.Technology;

import javax.persistence.*;

/**
 * Represents a specific technology alias.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class Alias extends SimplePersistentObject implements Cloneable
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
    @Column
    @Enumerated(EnumType.STRING)
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
    @Column
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

    /**
     * @return {@link #type#getTechnology()}
     */
    @Transient
    public Technology getTechnology()
    {
        return type.getTechnology();
    }

    @Override
    public Alias clone()
    {
        Alias alias = new Alias();
        alias.setType(type);
        alias.setValue(value);
        return alias;
    }

    /**
     * @return alias converted to API
     */
    public cz.cesnet.shongo.api.Alias toApi()
    {
        cz.cesnet.shongo.api.Alias api = new cz.cesnet.shongo.api.Alias();
        api.setId(getId());
        api.setType(getType());
        api.setValue(getValue());
        return api;
    }

    /**
     * Synchronize compartment from API
     *
     * @param api
     */
    public void fromApi(cz.cesnet.shongo.api.Alias api)
    {
        if (api.getType() != null) {
            setType(api.getType());
        }
        if (api.getValue() != null) {
            setValue(api.getValue());
        }
    }
}
