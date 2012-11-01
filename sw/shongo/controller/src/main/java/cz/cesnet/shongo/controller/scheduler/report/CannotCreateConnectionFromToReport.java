package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.executor.Endpoint;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class CannotCreateConnectionFromToReport extends AbstractConnectionReport
{
    /**
     * Constructor.
     */
    public CannotCreateConnectionFromToReport()
    {
    }

    /**
     * Constructor.
     *
     * @param endpointFrom
     * @param endpointTo
     */
    public CannotCreateConnectionFromToReport(Endpoint endpointFrom, Endpoint endpointTo)
    {
        super(endpointFrom, endpointTo);
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Cannot create connection from %s to %s.", getEndpointFromAsString(),
                getEndpointToAsString());
    }
}
