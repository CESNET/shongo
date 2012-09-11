package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.allocation.AllocatedEndpoint;
import cz.cesnet.shongo.controller.allocation.AllocatedExternalEndpoint;
import cz.cesnet.shongo.controller.allocation.AllocatedResource;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.resource.Resource;

import javax.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #toString()}
 */
@Entity
public abstract class AbstractResourceReport extends Report
{
    /**
     * Identification of resource.
     */
    private String resource;

    /**
     * Constructor.
     */
    public AbstractResourceReport()
    {
    }

    /**
     * Constructor.
     *
     * @param resource
     */
    public AbstractResourceReport(Resource resource)
    {
        this.resource = "resource(id: " + resource.getId() + ")";;
    }

    /**
     * @return {@link #resource}
     */
    @Column
    @Access(AccessType.FIELD)
    public String getResource()
    {
        return resource;
    }
}
