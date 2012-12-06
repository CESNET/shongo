package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Alias;

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
    private String aliasValue;

    /**
     * List of {@link Alias}es which are allocated by the {@link #aliasValue}.
     */
    private List<Alias> aliases = new ArrayList<Alias>();

    /**
     * @return {@link #aliasValue}
     */
    public String getAliasValue()
    {
        return aliasValue;
    }

    /**
     * @param aliasValue sets the {@link #aliasValue}
     */
    public void setAliasValue(String aliasValue)
    {
        this.aliasValue = aliasValue;
    }

    /**
     * @return {@link #aliases}
     */
    public List<Alias> getAliases()
    {
        return aliases;
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
}
