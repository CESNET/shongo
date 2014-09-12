package cz.cesnet.shongo.controller.booking.alias;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.AbstractComplexType;

import javax.persistence.*;
import java.util.*;

/**
 * Represents an identifier/number or any other value depicting some callable target.
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
    @Column(length = AbstractComplexType.ENUM_COLUMN_LENGTH)
    @Enumerated(EnumType.STRING)
    public AliasType getType()
    {
        return type;
    }

    /**
     * @return {@link AliasType#isCallable()}
     */
    @Transient
    public boolean isCallable()
    {
        return type.isCallable();
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
    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
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

    /**
     * @param callableAlias
     * @return true whether this {@link Alias} has higher callable priority than given {@code callableAlias},
     *         otherwise false
     */
    public boolean hasHigherCallPriorityThan(Alias callableAlias)
    {
        return type.compareTo(callableAlias.getType()) < 0;
    }

    @Override
    public Alias clone() throws CloneNotSupportedException
    {
        Alias alias = (Alias) super.clone();
        alias.setIdNull();
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

    public static void sort(List<Alias> aliases)
    {
        Collections.sort(aliases, new Comparator<Alias>()
        {
            @Override
            public int compare(Alias alias1, Alias alias2)
            {
                return alias1.getType().compareTo(alias2.getType());
            }
        });
    }

    public static List<Alias> sortedList(Collection<Alias> aliases)
    {
        List<Alias> aliasList = new LinkedList<Alias>(aliases);
        sort(aliasList);
        return aliasList;
    }
}
