package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.FaultException;

import java.util.List;

/**
 * Capability tells that the device is able to participate in a video conference call.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TerminalCapability extends Capability
{
    /**
     * List of aliases which the resource permanently has.
     */
    public static final String ALIASES = "aliases";

    /**
     * @return {@link #ALIASES}
     */
    public List<Alias> getAliases()
    {
        return getPropertyStorage().getCollection(ALIASES, List.class);
    }

    /**
     * @param aliases sets the {@link #ALIASES}
     */
    public void setAliases(List<Alias> aliases)
    {
        getPropertyStorage().setCollection(ALIASES, aliases);
    }

    /**
     * @param alias alias to be added to the {@link #ALIASES}
     */
    public void addAlias(Alias alias)
    {
        getPropertyStorage().addCollectionItem(ALIASES, alias, List.class);
    }

    /**
     * @param alias alias to be removed from the {@link #ALIASES}
     */
    public void removeAlias(Alias alias)
    {
        getPropertyStorage().removeCollectionItem(ALIASES, alias);
    }
}
