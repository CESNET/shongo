package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.DataMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a {@link Reservation} for an {@link Alias}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasReservation extends ResourceReservation
{
    /**
     * {@link Alias} which is allocated.
     */
    private ValueReservation valueReservation;

    /**
     * List of {@link Alias}es which are allocated by the {@link #valueReservation}.
     */
    private List<Alias> aliases = new ArrayList<Alias>();

    /**
     * @return {@link #valueReservation}
     */
    public ValueReservation getValueReservation()
    {
        return valueReservation;
    }

    /**
     * @param valueReservation sets the {@link #valueReservation}
     */
    public void setValueReservation(ValueReservation valueReservation)
    {
        this.valueReservation = valueReservation;
    }

    /**
     * @return {@link #valueReservation#getValue()}
     */
    public String getValue()
    {
        return valueReservation.getValue();
    }

    /**
     * @return {@link #aliases}
     */
    public List<Alias> getAliases()
    {
        return aliases;
    }

    /**
     * @param aliasType
     * @return {@link Alias} with given {@code aliasType}
     */
    public Alias getAlias(AliasType aliasType)
    {
        for (Alias alias : aliases) {
            if (alias.getType().equals(aliasType)) {
                return alias;
            }
        }
        throw new IllegalArgumentException(aliasType.toString() + " alias doesn't exist.");
    }

    /**
     * @param aliases {@link #aliases}
     */
    public void setAliases(List<Alias> aliases)
    {
        this.aliases = aliases;
    }

    /**
     * @param alias to be added to the {@link #aliases}
     */
    public void addAlias(Alias alias)
    {
        aliases.add(alias);
    }

    private static final String VALUE_RESERVATION = "valueReservation";
    private static final String ALIASES = "aliases";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(VALUE_RESERVATION, valueReservation);
        dataMap.set(ALIASES, aliases);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        valueReservation = dataMap.getComplexType(VALUE_RESERVATION, ValueReservation.class);
        aliases = dataMap.getList(ALIASES, Alias.class);
    }
}
