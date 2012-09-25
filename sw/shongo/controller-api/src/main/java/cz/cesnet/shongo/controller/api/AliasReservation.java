package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Alias;

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
