package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Alias;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a {@link ResourceReservation} for an endpoint {@link Resource}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class EndpointReservation extends ResourceReservation
{
    /**
     * {@link Alias}es that are additionally assigned to the device.
     */
    private List<Alias> aliases = new ArrayList<Alias>();

    /**
     * @return {@link #aliases}
     */
    public List<Alias> getAliases()
    {
        return aliases;
    }

    /**
     * @param alias alias to be added to the {@link #aliases}
     */
    public void addAlias(Alias alias)
    {
        aliases.add(alias);
    }
}
