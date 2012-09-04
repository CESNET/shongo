package cz.cesnet.shongo.controller.resource.database;

import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.Resource;

/**
 * Represents an available {@link cz.cesnet.shongo.controller.resource.Alias} in a {@link cz.cesnet.shongo.controller.resource.Resource} with {@link cz.cesnet.shongo.controller.resource.AliasProviderCapability}.
 */
public class AvailableAlias
{
    /**
     * Available {@link cz.cesnet.shongo.controller.resource.Alias}
     */
    private Alias alias;

    /**
     * {@link cz.cesnet.shongo.controller.resource.Resource} in which the {@link #alias} is available.
     */
    private Resource resource;

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

    /**
     * @return {@link #resource}
     */
    public Resource getResource()
    {
        return resource;
    }

    /**
     * @param resource sets the {@link #resource}
     */
    public void setResource(Resource resource)
    {
        this.resource = resource;
    }
}
