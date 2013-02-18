package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.resource.Resource;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class ResourceNotAllocatableReport extends ResourceReport
{
    /**
     * Constructor.
     */
    public ResourceNotAllocatableReport()
    {
    }

    /**
     * Constructor.
     *
     * @param resource
     */
    public ResourceNotAllocatableReport(Resource resource)
    {
        super(resource);
    }

    @Override
    @Transient
    public State getState()
    {
        return State.ERROR;
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("%s is not allocatable.", getResourceDescription(true));
    }
}
