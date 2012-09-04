package cz.cesnet.shongo.controller.resource.database;

import cz.cesnet.shongo.controller.allocation.AllocatedAlias;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.Resource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasManager
{
    /**
     * Map of {@link AliasProviderCapability}s by resource identifier (used for removing all capabilities
     * of a given resource).
     */
    private Map<Long, Set<AliasProviderCapability>> aliasProviderCapabilitiesByResourceId =
            new HashMap<Long, Set<AliasProviderCapability>>();

    /**
     * Map of {@link AliasProviderState} by identifier of {@link AliasProviderCapability}.
     */
    private Map<Long, AliasProviderState> aliasProviderStateById = new HashMap<Long, AliasProviderState>();

    /**
     * Add new {@link AliasProviderCapability} to be managed by the {@link AliasManager}.
     *
     * @param aliasProviderCapability
     */
    public void addAliasProvider(AliasProviderCapability aliasProviderCapability)
    {
        Resource resource = aliasProviderCapability.getResource();
        Long resourceId = resource.getId();

        // Store capability for removing by resource
        Set<AliasProviderCapability> aliasProviderCapabilities = aliasProviderCapabilitiesByResourceId.get(resourceId);
        if (aliasProviderCapabilities == null) {
            aliasProviderCapabilities = new HashSet<AliasProviderCapability>();
            aliasProviderCapabilitiesByResourceId.put(resourceId, aliasProviderCapabilities);
        }
        aliasProviderCapabilities.add(aliasProviderCapability);

        // Create alias provider state for given capability
        aliasProviderStateById.put(aliasProviderCapability.getId(), new AliasProviderState());
    }

    /**
     * Remove all managed {@link AliasProviderCapability}s from given {@code resource} from the {@link AliasManager}.
     *
     * @param resource
     */
    public void removeAliasProviders(Resource resource)
    {
        Long resourceId = resource.getId();

        // Remove all states for alias providers
        Set<AliasProviderCapability> aliasProviderCapabilities = aliasProviderCapabilitiesByResourceId.get(resourceId);
        for (AliasProviderCapability aliasProviderCapability : aliasProviderCapabilities) {
            aliasProviderStateById.remove(aliasProviderCapability.getId());
        }

        // Remove all capabilities by resource identifier
        aliasProviderCapabilitiesByResourceId.remove(resourceId);
    }


    public void addAllocatedAlias(AllocatedAlias allocatedAlias)
    {
        Long aliasProviderId = allocatedAlias.getAliasProviderCapability().getId();
        AliasProviderState aliasProviderState = aliasProviderStateById.get(aliasProviderId);
        if (aliasProviderState == null) {
            throw new IllegalStateException("Alias provider is not maintained by the alias manager.");
        }

        // todo:
    }

    public void removeAllocatedAlias(AllocatedAlias allocatedAlias)
    {
        Long aliasProviderId = allocatedAlias.getAliasProviderCapability().getId();
        AliasProviderState aliasProviderState = aliasProviderStateById.get(aliasProviderId);
        if (aliasProviderState == null) {
            throw new IllegalStateException("Alias provider is not maintained by the alias manager.");
        }

        // todo:
    }
}
