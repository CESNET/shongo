package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.controller.allocation.AllocatedAlias;
import cz.cesnet.shongo.controller.allocation.AllocatedResource;
import cz.cesnet.shongo.controller.util.RangeSet;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasProviderState
{
    /**
     * Already allocated {@link AllocatedAlias}s for the resource.
     */
    private RangeSet<AllocatedAlias, DateTime> allocatedAliases = new RangeSet<AllocatedAlias, DateTime>();

    /**
     * Map of {@link AllocatedAlias}s by the identifier.
     */
    private Map<Long, AllocatedAlias> allocatedAliasesById = new HashMap<Long, AllocatedAlias>();

    /**
     * @param allocatedAlias to be added to the {@link AliasProviderState}
     */
    public void addAllocatedAlias(AllocatedAlias allocatedAlias)
    {
        Interval slot = allocatedAlias.getSlot();
        allocatedAliases.add(allocatedAlias, slot.getStart(), slot.getEnd());
    }

    /**
     * @param allocatedAlias to be removed from the {@link AliasProviderState}
     */
    public void removeAllocatedAlias(AllocatedAlias allocatedAlias)
    {
        Long allocatedAliasId = allocatedAlias.getId();
        allocatedAlias = allocatedAliasesById.get(allocatedAliasId);
        if (allocatedAlias == null) {
            throw new IllegalStateException("Allocated alias doesn't exist in the cache.");
        }
        allocatedAliases.remove(allocatedAlias);
        allocatedAliasesById.remove(allocatedAliasId);
    }

    /**
     * Clear all {@link AllocatedAlias} from the {@link AliasProviderState}.
     */
    public void clear()
    {
        allocatedAliases.clear();
        allocatedAliasesById.clear();
    }
}
