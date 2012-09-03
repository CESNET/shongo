package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.Technology;

import javax.persistence.*;

/**
 * Represents a connection between two {@link cz.cesnet.shongo.controller.allocation.AllocatedEndpoint}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Connection extends PersistentObject
{
    /**
     * Source endpoint.
     */
    private AllocatedItem allocatedEndpointFrom;

    /**
     * Target endpoint.
     */
    private AllocatedItem allocatedEndpointTo;

    /**
     * @return {@link #allocatedEndpointFrom}
     */
    @ManyToOne
    @JoinColumn(name = "allocated_endpoint_from_id")
    public AllocatedItem getAllocatedEndpointFrom()
    {
        return allocatedEndpointFrom;
    }

    /**
     * @param allocatedEndpointFrom sets the {@link #allocatedEndpointFrom}
     */
    public void setAllocatedEndpointFrom(AllocatedItem allocatedEndpointFrom)
    {
        if (!(allocatedEndpointFrom instanceof AllocatedEndpoint)) {
            throw new IllegalArgumentException("Given allocated item is not endpoint!");
        }
        this.allocatedEndpointFrom = allocatedEndpointFrom;
    }

    /**
     * @return {@link #allocatedEndpointTo}
     */
    @ManyToOne
    @JoinColumn(name = "allocated_endpoint_to_id")
    public AllocatedItem getAllocatedEndpointTo()
    {
        return allocatedEndpointTo;
    }

    /**
     * @param allocatedEndpointTo sets the {@link #allocatedEndpointTo}
     */
    public void setAllocatedEndpointTo(AllocatedItem allocatedEndpointTo)
    {
        if (!(allocatedEndpointFrom instanceof AllocatedEndpoint)) {
            throw new IllegalArgumentException("Given allocated item is not endpoint!");
        }
        this.allocatedEndpointTo = allocatedEndpointTo;
    }
}
