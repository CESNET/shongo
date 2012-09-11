package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.resource.Resource;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #toString()}
 */
@Entity
public class ResourceNotAllocatableReport extends AbstractResourceReport
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
    public String getText()
    {
        return String.format("Resource %s is not allocatable.", getResource());
    }
}
