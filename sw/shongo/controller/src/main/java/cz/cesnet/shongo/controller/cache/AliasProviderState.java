package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.controller.allocation.AllocatedAlias;
import cz.cesnet.shongo.controller.util.RangeSet;
import org.joda.time.DateTime;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasProviderState
{
    /**
     * Already allocated {@link cz.cesnet.shongo.controller.allocation.AllocatedResource}s for the resource.
     */
    private RangeSet<AllocatedAlias, DateTime> allocatedAliases = new RangeSet<AllocatedAlias, DateTime>();
}
