package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.booking.domain.Domain;

/**
 * Represents a cache for all domains in efficient form.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DomainCache extends AbstractCache<Domain>
{
    /**
     * Constructor.
     */
    public DomainCache()
    {
    }

    public Domain getDomainByName(String domainName)
    {
        for (Domain domain : getObjects())
        {
            if (domain.getName().equals(domainName)) {
                return domain;
            }
        }
        return null;
    }
}
