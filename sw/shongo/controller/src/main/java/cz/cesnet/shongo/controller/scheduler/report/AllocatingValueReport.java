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
public class AllocatingValueReport extends ResourceReport
{
    /**
     * Constructor.
     */
    public AllocatingValueReport()
    {
    }

    /**
     * Constructor.
     *
     * @param resource sets the {@link #resource}
     */
    public AllocatingValueReport(Resource resource)
    {
        super(resource);
    }

    /**
     * Constructor.
     *
     * @param capability sets the {@link #capability}
     */
    public AllocatingValueReport(Capability capability)
    {
        super(capability);
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Allocating value in %s", getResourceDescription(false));
    }
}
