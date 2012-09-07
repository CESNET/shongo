package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Alias;

/**
 * Represents an allocated alias.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocatedAlias extends AllocatedItem
{
    /**
     * Alias which is allocated.
     */
    private Alias alias;

    /**
     * @return {@link #alias}
     */
    public Alias getAlias()
    {
        return alias;
    }

    /**
     * @param alias sets the {@link #alias}
     */
    public void setAlias(Alias alias)
    {
        this.alias = alias;
    }
}
