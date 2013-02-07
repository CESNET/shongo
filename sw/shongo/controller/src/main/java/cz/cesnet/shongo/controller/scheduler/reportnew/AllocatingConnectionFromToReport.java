package cz.cesnet.shongo.controller.scheduler.reportnew;

import cz.cesnet.shongo.controller.executor.Endpoint;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class AllocatingConnectionFromToReport extends AbstractConnectionReport
{
    /**
     * Constructor.
     */
    public AllocatingConnectionFromToReport()
    {
    }

    /**
     * Constructor.
     *
     * @param endpointFrom
     * @param endpointTo
     */
    public AllocatingConnectionFromToReport(Endpoint endpointFrom, Endpoint endpointTo)
    {
        super(endpointFrom, endpointTo);
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Creating connection from %s to %s.", getEndpointFromAsString(),
                getEndpointToAsString());
    }
}
