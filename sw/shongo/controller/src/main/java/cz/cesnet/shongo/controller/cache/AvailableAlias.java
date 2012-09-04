package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.Resource;

/**
 * Represents an available {@link Alias} in a {@link Resource} with {@link AliasProviderCapability}.
 */
public class AvailableAlias
{
    /**
     * Available {@link Alias}
     */
    private Alias alias;

    /**
     * {@link AliasProviderCapability} in which the {@link #alias} is available.
     */
    private AliasProviderCapability aliasProviderCapability;

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
     * @return {@link #aliasProviderCapability}
     */
    public AliasProviderCapability getAliasProviderCapability()
    {
        return aliasProviderCapability;
    }

    /**
     * @param aliasProviderCapability sets the {@link #aliasProviderCapability}
     */
    public void setAliasProviderCapability(AliasProviderCapability aliasProviderCapability)
    {
        this.aliasProviderCapability = aliasProviderCapability;
    }
}
