package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.request.ReservationRequest;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an allocation for a {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class Reservation extends PersistentObject
{
    /**
     * {@link ReservationRequest} for which the reservation is allocated.
     */
    private ReservationRequest reservationRequest;

    /**
     * Items that are allocated for the {@link #reservationRequest}.
     */
    private List<AllocatedItem> allocatedItems = new ArrayList<AllocatedItem>();

    /**
     * List of connections which will be initiated in the plan.
     */
    List<Connection> connections = new ArrayList<Connection>();

    /**
     * @return {@link #reservationRequest}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public ReservationRequest getReservationRequest()
    {
        return reservationRequest;
    }

    /**
     * @param reservationRequest sets the {@link #reservationRequest}
     */
    public void setReservationRequest(ReservationRequest reservationRequest)
    {
        this.reservationRequest = reservationRequest;
    }

    /**
     * @return {@link #allocatedItems}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<AllocatedItem> getAllocatedItems()
    {
        return allocatedItems;
    }

    /**
     * @param allocatedItem allocated item to be added to the {@link #allocatedItems}
     */
    public void addAllocatedItem(AllocatedItem allocatedItem)
    {
        allocatedItems.add(allocatedItem);
    }

    /**
     * @param allocatedItem allocated item to be removed from the {@link #allocatedItems}
     */
    public void removeAllocatedItem(AllocatedItem allocatedItem)
    {
        allocatedItems.remove(allocatedItem);
    }

    /**
     * @return {@link #allocatedItems}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<Connection> getConnections()
    {
        return connections;
    }

    /**
     * @param connection to be added to the {@link #connections}
     */
    public void addConnection(Connection connection)
    {
        connections.add(connection);
    }

    /**
     * @param domain
     * @return allocated compartment converted to API
     */
    /*public cz.cesnet.shongo.controller.api.AllocatedCompartment toApi(Domain domain)
    {
        CompartmentRequest compartmentRequest = getCompartmentRequest();

        cz.cesnet.shongo.controller.api.AllocatedCompartment apiAllocatedCompartment =
                new cz.cesnet.shongo.controller.api.AllocatedCompartment();
        apiAllocatedCompartment.setSlot(compartmentRequest.getRequestedSlot());
        for (AllocatedItem allocatedItem : allocatedItems) {
            cz.cesnet.shongo.controller.api.AllocatedItem apiAllocatedItem = allocatedItem.toApi(domain);
            if (apiAllocatedItem != null) {
                apiAllocatedCompartment.addAllocatedItem(apiAllocatedItem);
            }
        }
        return apiAllocatedCompartment;
    }*/
}
