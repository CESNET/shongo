package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.resource.Resource;

import javax.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #toString()}
 */
@Entity
public class ResourceRequestedMultipleTimesReport extends AbstractResourceReport
{
    /**
     * Constructor.
     */
    public ResourceRequestedMultipleTimesReport()
    {
    }

    /**
     * Constructor.
     *
     * @param resource
     */
    public ResourceRequestedMultipleTimesReport(Resource resource)
    {
        super(resource);
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Resource %s is requested multiple times.", getResource());
    }
}
