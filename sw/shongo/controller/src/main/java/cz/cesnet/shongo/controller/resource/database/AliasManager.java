package cz.cesnet.shongo.controller.resource.database;

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
    private Map<Long, Set<AliasProviderCapability>> aliasProviderCapabilitiesByResourceId =
            new HashMap<Long, Set<AliasProviderCapability>>();

    public void addAliasProvider(AliasProviderCapability aliasProviderCapability)
    {
        Resource resource = aliasProviderCapability.getResource();
        Long resourceId = resource.getId();

        Set<AliasProviderCapability> aliasProviderCapabilities = aliasProviderCapabilitiesByResourceId.get(resourceId);
        if (aliasProviderCapabilities == null) {
            aliasProviderCapabilities = new HashSet<AliasProviderCapability>();
            aliasProviderCapabilitiesByResourceId.put(resourceId, aliasProviderCapabilities);
        }
        aliasProviderCapabilities.add(aliasProviderCapability);

        // todo:
    }

    public void removeAliasProviders(Resource resource)
    {
        Long resourceId = resource.getId();
        Set<AliasProviderCapability> aliasProviderCapabilities = aliasProviderCapabilitiesByResourceId.get(resourceId);
        for (AliasProviderCapability aliasProviderCapability : aliasProviderCapabilities) {
            // todo:
        }
        aliasProviderCapabilitiesByResourceId.remove(resourceId);
    }
}
