package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.PersistentObject;

import javax.persistence.*;

/**
 * Represents a connection (e.g., audio channel, video channel, etc.) between two {@link Endpoint}s
 * in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Connection extends PersistentObject
{
    /**
     * The {@link Endpoint} which initiates the connection.
     */
    private Endpoint endpointFrom;

    /**
     * The {@link Endpoint} which accepts incoming connection.
     */
    private Endpoint endpointTo;

    /**
     * @return {@link #endpointFrom}
     */
    @ManyToOne
    @JoinColumn(name = "endpoint_from_id")
    public Endpoint getEndpointFrom()
    {
        return endpointFrom;
    }

    /**
     * @param endpointFrom sets the {@link #endpointFrom}
     */
    public void setEndpointFrom(Endpoint endpointFrom)
    {
        this.endpointFrom = endpointFrom;
    }

    /**
     * @return {@link #endpointTo}
     */
    @ManyToOne
    @JoinColumn(name = "endpoint_to_id")
    public Endpoint getEndpointTo()
    {
        return endpointTo;
    }

    /**
     * @param endpointTo sets the {@link #endpointTo}
     */
    public void setEndpointTo(Endpoint endpointTo)
    {
        this.endpointTo = endpointTo;
    }

    /**
     * Establish connection between {@link #endpointFrom} and {@link #endpointTo}.
     *
     * @param compartmentExecutor
     */
    public abstract void establish(CompartmentExecutor compartmentExecutor);

    /**
     * Close connection between {@link #endpointFrom} and {@link #endpointTo}.
     *
     * @param compartmentExecutor
     */
    public abstract void close(CompartmentExecutor compartmentExecutor);
}
