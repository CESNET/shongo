package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.oldapi.rpc.StructType;

/**
 * Represents a base information about controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Controller implements StructType
{
    /**
     * Controller domain.
     */
    private Domain domain;

    /**
     * @return {@link #domain}
     */
    public Domain getDomain()
    {
        return domain;
    }

    /**
     * @param domain sets the {@link #domain}
     */
    public void setDomain(Domain domain)
    {
        this.domain = domain;
    }
}
