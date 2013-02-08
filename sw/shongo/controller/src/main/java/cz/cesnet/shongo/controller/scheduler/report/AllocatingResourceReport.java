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
public class AllocatingResourceReport extends AbstractResourceReport
{
    /**
     * Constructor.
     */
    public AllocatingResourceReport()
    {
    }

    /**
     * Constructor.
     *
     * @param resource sets the {@link #resource}
     */
    public AllocatingResourceReport(Resource resource)
    {
        super(resource);
    }

    /**
     * Constructor.
     *
     * @param capability sets the {@link #capability}
     */
    public AllocatingResourceReport(Capability capability)
    {
        super(capability);
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Allocating %s", getResourceDescription());
    }
}
