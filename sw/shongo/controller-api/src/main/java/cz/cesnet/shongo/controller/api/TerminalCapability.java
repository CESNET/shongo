package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.DataMap;

import java.util.LinkedList;
import java.util.List;

/**
 * Capability tells that the device is able to participate in a conference call.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TerminalCapability extends Capability
{
    /**
     * List of aliases which the resource permanently has.
     */
    private List<Alias> aliases = new LinkedList<Alias>();

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

    /**
     * @param alias alias to be removed from the {@link #aliases}
     */
    public void removeAlias(Alias alias)
    {
        aliases.remove(alias);
    }

    public static final String ALIASES = "aliases";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ALIASES, aliases);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        aliases = dataMap.getList(ALIASES, Alias.class);
    }
}
