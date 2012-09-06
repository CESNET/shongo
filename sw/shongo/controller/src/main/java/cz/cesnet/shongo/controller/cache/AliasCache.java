package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.controller.allocation.AllocatedAlias;
import cz.cesnet.shongo.controller.resource.*;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Represents a cache of allocated aliases.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasCache extends AbstractAllocationCache<AliasProviderCapability, AllocatedAlias>
{
    private static Logger logger = LoggerFactory.getLogger(AliasCache.class);

    /**
     * Map of {@link AliasProviderCapability}s by resource identifier (used for removing all capabilities
     * of a given resource).
     */
    private Map<Long, Set<AliasProviderCapability>> aliasProviderCapabilitiesByResourceId =
            new HashMap<Long, Set<AliasProviderCapability>>();

    @Override
    public void loadObjects(EntityManager entityManager)
    {
        logger.debug("Loading alias providers...");

        ResourceManager resourceManager = new ResourceManager(entityManager);
        List<AliasProviderCapability> aliasProviders = resourceManager.listCapabilities(AliasProviderCapability.class);
        for (AliasProviderCapability aliasProvider : aliasProviders) {
            addObject(aliasProvider, entityManager);
        }
    }

    @Override
    public void addObject(AliasProviderCapability aliasProviderCapability, EntityManager entityManager)
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

        super.addObject(aliasProviderCapability, entityManager);
    }

    @Override
    public void removeObject(AliasProviderCapability object)
    {
        super.removeObject(object);
    }

    /**
     * Remove all managed {@link AliasProviderCapability}s from given {@code resource} from the {@link AliasCache}.
     *
     * @param resource
     */
    public void removeAliasProviders(Resource resource)
    {
        Long resourceId = resource.getId();

        // Remove all states for alias providers
        Set<AliasProviderCapability> aliasProviderCapabilities = aliasProviderCapabilitiesByResourceId.get(resourceId);
        if (aliasProviderCapabilities != null) {
            for (AliasProviderCapability aliasProviderCapability : aliasProviderCapabilities) {
                removeObject(aliasProviderCapability);
            }
        }
    }

    @Override
    protected void updateObjectState(AliasProviderCapability object, Interval workingInterval,
            EntityManager entityManager)
    {
        // Get all allocated aliases for the alias provider and add them to the device state
        ResourceManager resourceManager = new ResourceManager(entityManager);
        List<AllocatedAlias> allocations = resourceManager.listAllocatedAliasesInInterval(object.getId(),
                getWorkingInterval());
        for (AllocatedAlias allocatedAlias : allocations) {
            addAllocation(object, allocatedAlias);
        }
    }

    /**
     * Find available alias in given {@code aliasProviderCapability}.
     *
     * @param aliasProviderCapability
     * @param interval
     * @param transaction
     * @return available alias for given {@code interval} from given {@code aliasProviderCapability}
     */
    public AvailableAlias getAvailableAlias(AliasProviderCapability aliasProviderCapability, Interval interval,
            Transaction transaction)
    {
        ObjectState<AllocatedAlias> aliasProviderState = getObjectState(aliasProviderCapability);
        Set<AllocatedAlias> allocatedAliases = aliasProviderState.getAllocations(interval, transaction);
        AliasGenerator aliasGenerator = aliasProviderCapability.getAliasGenerator();
        for (AllocatedAlias allocatedAlias : allocatedAliases) {
            aliasGenerator.addAlias(allocatedAlias.getAlias());
        }
        Alias alias = aliasGenerator.generate();
        if (alias == null) {
            return null;
        }
        AvailableAlias availableAlias = new AvailableAlias();
        availableAlias.setAliasProviderCapability(aliasProviderCapability);
        availableAlias.setAlias(alias);
        return availableAlias;
    }

    /**
     * Transaction for {@link AliasCache}.
     */
    public static class Transaction
            extends AbstractAllocationCache.Transaction<AllocatedAlias>
    {
    }
}
