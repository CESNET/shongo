package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Alias;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an allocation of a {@link DeviceResource} for a {@link Compartment} in the {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocatedDevice extends AllocatedResource
{
    /**
     * Aliases that are additionally assigned to the device.
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
