package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.allocationaold.AllocatedEndpoint;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class CannotCreateConnectionFromToMultipleReport extends AbstractConnectionReport
{
    /**
     * Constructor.
     */
    public CannotCreateConnectionFromToMultipleReport()
    {
    }

    /**
     * Constructor.
     *
     * @param endpointFrom
     * @param endpointTo
     */
    public CannotCreateConnectionFromToMultipleReport(AllocatedEndpoint endpointFrom, AllocatedEndpoint endpointTo)
    {
        super(endpointFrom, endpointTo);
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Cannot create connection from %s to %s, "
                + "because the target represents multiple endpoints (not supported yet).",
                getEndpointFrom(), getEndpointTo());
    }
}
