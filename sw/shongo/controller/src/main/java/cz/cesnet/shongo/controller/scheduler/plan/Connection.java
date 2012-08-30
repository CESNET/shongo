package cz.cesnet.shongo.controller.scheduler.plan;

import cz.cesnet.shongo.Technology;

/**
 * Represents a connection between two {@link Endpoint}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Connection
{
    /**
     * Source endpoint.
     */
    private Endpoint endpointFrom;

    /**
     * Target endpoint.
     */
    private Endpoint endpointTo;

    /**
     * Technology of connection.
     */
    private Technology technology;

    /**
     * Constructor.
     *
     * @param endpointFrom sets the {@link #endpointFrom}
     * @param endpointTo   sets the {@link #endpointTo}
     * @param technology   sets the {@link #technology}
     */
    Connection(Endpoint endpointFrom, Endpoint endpointTo, Technology technology)
    {
        this.technology = technology;
        this.endpointFrom = endpointFrom;
        this.endpointTo = endpointTo;
    }

    /**
     * @return {@link #endpointFrom}
     */
    public Endpoint getEndpointFrom()
    {
        return endpointFrom;
    }

    /**
     * @return {@link #endpointTo}
     */
    public Endpoint getEndpointTo()
    {
        return endpointTo;
    }

    /**
     * @return {@link #technology}
     */
    public Technology getTechnology()
    {
        return technology;
    }
}
