package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.allocation.AllocatedEndpoint;
import cz.cesnet.shongo.controller.allocation.AllocatedExternalEndpoint;
import cz.cesnet.shongo.controller.allocation.AllocatedResource;
import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #toString()}
 */
@Entity
public abstract class AbstractConnectionReport extends Report
{
    /**
     * Identification of source endpoint.
     */
    private String endpointFrom;

    /**
     * Identification of target endpoint.
     */
    private String endpointTo;

    /**
     * Constructor.
     */
    public AbstractConnectionReport()
    {
    }

    /**
     * Constructor.
     *
     * @param endpointFrom
     * @param endpointTo
     */
    public AbstractConnectionReport(AllocatedEndpoint endpointFrom, AllocatedEndpoint endpointTo)
    {
        this.endpointFrom = getEndpoint(endpointFrom);
        this.endpointTo = getEndpoint(endpointTo);
    }

    @Transient
    private String getEndpoint(AllocatedEndpoint allocatedEndpoint)
    {
        if (allocatedEndpoint instanceof AllocatedResource) {
            AllocatedResource allocatedResource = (AllocatedResource) allocatedEndpoint;
            return "resource(id: " + allocatedResource.getResource().getId() + ")";
        }
        else if (allocatedEndpoint instanceof AllocatedExternalEndpoint) {
            AllocatedExternalEndpoint allocatedExternalEndpoint = (AllocatedExternalEndpoint) allocatedEndpoint;
            return "external endpoint(count: "
                    + allocatedExternalEndpoint.getExternalEndpointSpecification().getCount() + ")";
        }
        else {
            throw new IllegalArgumentException("Unknown type of allocated endpoint '"
                    + allocatedEndpoint.getClass().getSimpleName() + "'.");
        }
    }

    /**
     * @return {@link #endpointFrom}
     */
    @Column
    @Access(AccessType.FIELD)
    public String getEndpointFrom()
    {
        return endpointFrom;
    }

    /**
     * @return {@link #endpointTo}
     */
    @Column
    @Access(AccessType.FIELD)
    public String getEndpointTo()
    {
        return endpointTo;
    }
}
