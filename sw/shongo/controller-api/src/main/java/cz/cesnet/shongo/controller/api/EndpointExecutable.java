package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.DataMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an endpoint which is participating in the {@link cz.cesnet.shongo.controller.api.CompartmentExecutable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class EndpointExecutable extends Executable
{
    /**
     * Description of the {@link cz.cesnet.shongo.controller.api.EndpointExecutable}.
     */
    private String description;

    /**
     * List of assigned {@link cz.cesnet.shongo.api.Alias}es to the {@link cz.cesnet.shongo.controller.api.EndpointExecutable}.
     */
    private List<Alias> aliases = new ArrayList<Alias>();

    /**
     * @return {@link #description}
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description sets the {@link #description}
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return {@link #aliases}
     */
    public List<Alias> getAliases()
    {
        return aliases;
    }

    /**
     * @param aliases sets the {@link #aliases}
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

    private static final String DESCRIPTION = "description";
    private static final String ALIASES = "aliases";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(DESCRIPTION, description);
        dataMap.set(ALIASES, aliases);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        description = dataMap.getString(DESCRIPTION);
        aliases = dataMap.getList(ALIASES, Alias.class);
    }
}
