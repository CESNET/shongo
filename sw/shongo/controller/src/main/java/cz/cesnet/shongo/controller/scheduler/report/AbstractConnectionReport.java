package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.allocationaold.AllocatedEndpoint;
import cz.cesnet.shongo.controller.compartment.Endpoint;
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
    private Endpoint endpointFrom;

    /**
     * Identification of target endpoint.
     */
    private Endpoint endpointTo;

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
    public AbstractConnectionReport(Endpoint endpointFrom, Endpoint endpointTo)
    {
        this.endpointFrom = endpointFrom;
        this.endpointTo = endpointTo;
    }

    /**
     * @return {@link #endpointFrom}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public Endpoint getEndpointFrom()
    {
        return endpointFrom;
    }

    /**
     * @return {@link #endpointFrom} as string
     */
    @Transient
    public String getEndpointFromAsString()
    {
        return endpointFrom.getReportDescription();
    }

    /**
     * @return {@link #endpointTo}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public Endpoint getEndpointTo()
    {
        return endpointTo;
    }

    /**
     * @return {@link #endpointFrom} as string
     */
    @Transient
    public String getEndpointToAsString()
    {
        return endpointTo.getReportDescription();
    }
}
