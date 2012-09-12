package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.allocation.AllocatedEndpoint;
import cz.cesnet.shongo.controller.allocation.AllocatedExternalEndpoint;
import cz.cesnet.shongo.controller.allocation.AllocatedResource;
import cz.cesnet.shongo.controller.allocation.AllocatedVirtualRoom;
import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.*;

/**
 * Represents a {@link Report} for connection between two {@link AllocatedEndpoint}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
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
        if (allocatedEndpoint instanceof AllocatedVirtualRoom) {
            AllocatedVirtualRoom allocatedVirtualRoom = (AllocatedVirtualRoom) allocatedEndpoint;
            return String.format("virtual room on %s",
                    AbstractResourceReport.formatResource(allocatedVirtualRoom.getResource()));
        }
        if (allocatedEndpoint instanceof AllocatedResource) {
            AllocatedResource allocatedResource = (AllocatedResource) allocatedEndpoint;
            return AbstractResourceReport.formatResource(allocatedResource.getResource());
        }
        else if (allocatedEndpoint instanceof AllocatedExternalEndpoint) {
            AllocatedExternalEndpoint allocatedExternalEndpoint = (AllocatedExternalEndpoint) allocatedEndpoint;
            return String.format("external endpoint(count: %d)",
                    allocatedExternalEndpoint.getExternalEndpointSpecification().getCount());
        }
        else {
            return allocatedEndpoint.toString();
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
