package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.resource.Capability;
import cz.cesnet.shongo.controller.resource.Resource;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class CheckingResourceReport extends AbstractResourceReport
{
    /**
     * Constructor.
     */
    public CheckingResourceReport()
    {
    }

    /**
     * Constructor.
     *
     * @param resource sets the {@link #resource}
     */
    public CheckingResourceReport(Resource resource)
    {
        super(resource);
    }

    /**
     * Constructor.
     *
     * @param capability sets the {@link #capability}
     */
    public CheckingResourceReport(Capability capability)
    {
        super(capability);
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Checking %s.", getResourceDescription());
    }
}
