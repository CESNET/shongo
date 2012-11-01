package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Alias;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an allocated object which can be executed.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class VirtualRoom extends Executable
{
    /**
     * Identifier of the {@link Resource}.
     */
    private String resourceIdentifier;

    /**
     * Number of available ports in the {@link VirtualRoom}.
     */
    private int portCount;

    /**
     * List of assigned {@link cz.cesnet.shongo.api.Alias}es to the {@link VirtualRoom}.
     */
    private List<Alias> aliases = new ArrayList<Alias>();

    /**
     * @return {@link #resourceIdentifier}
     */
    public String getResourceIdentifier()
    {
        return resourceIdentifier;
    }

    /**
     * @param resourceIdentifier sets the {@link #resourceIdentifier}
     */
    public void setResourceIdentifier(String resourceIdentifier)
    {
        this.resourceIdentifier = resourceIdentifier;
    }

    /**
     * @return {@link #portCount}
     */
    public int getPortCount()
    {
        return portCount;
    }

    /**
     * @param portCount sets the {@link #portCount}
     */
    public void setPortCount(int portCount)
    {
        this.portCount = portCount;
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
}
