package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.resource.Resource;

import javax.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class ResourceNotMatchRestrictedReport extends AbstractResourceReport
{
    /**
     * Constructor.
     */
    public ResourceNotMatchRestrictedReport()
    {
    }

    /**
     * Constructor.
     */
    public ResourceNotMatchRestrictedReport(Resource resource)
    {
        super(resource);
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("The %s not match restricted.", getResourceDescription());
    }
}
