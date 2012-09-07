package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.Technology;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.Map;

/**
 * Represents a specific technology alias.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class Alias extends PersistentObject
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
    public void setTechnology(Technology technology)
    {
        this.technology = technology;
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
     * @return alias converted to API
     */
    public cz.cesnet.shongo.api.Alias toApi()
    {
        cz.cesnet.shongo.api.Alias api = new cz.cesnet.shongo.api.Alias();
        api.setId(getId().intValue());
        api.setTechnology(getTechnology());
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
        if (api.getTechnology() != null) {
            setTechnology(api.getTechnology());
        }
        if (api.getType() != null) {
            setType(api.getType());
        }
        if (api.getValue() != null) {
            setValue(api.getValue());
        }
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);
        map.put("technology", technology.getName());
        map.put("type", type.toString());
        map.put("value", value);
    }
}
