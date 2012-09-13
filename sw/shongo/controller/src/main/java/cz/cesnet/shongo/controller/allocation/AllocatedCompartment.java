package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.Domain;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents allocated resources for a single compartment request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AllocatedCompartment extends PersistentObject
{
    /**
     * {@link Reservation} for which the compartment is allocated.
     */
    private Reservation reservation;

    /**
     * {@link CompartmentRequest} for which the resources are allocated.
     */
    private CompartmentRequest compartmentRequest;

    /**
     * Items that are allocated for the {@link #compartmentRequest}.
     */
    private List<AllocatedItem> allocatedItems = new ArrayList<AllocatedItem>();

    /**
     * List of connections which will be initiated in the plan.
     */
    List<Connection> connections = new ArrayList<Connection>();

    /**
     * @return {@link #reservation}
     */
    @ManyToOne
    @Access(AccessType.FIELD)
    public Reservation getReservation()
    {
        return reservation;
    }

    /**
     * @param reservation sets the {@link #reservation}
     */
    public void setReservation(Reservation reservation)
    {
        // Manage bidirectional association
        if (reservation != this.reservation) {
            if (this.reservation != null) {
                Reservation oldReservation = this.reservation;
                this.reservation = null;
                oldReservation.removeAllocatedCompartment(this);
            }
            if (reservation != null) {
                this.reservation = reservation;
                this.reservation.addAllocatedCompartment(this);
            }
        }
        this.reservation = reservation;
    }

    /**
     * @return {@link #compartmentRequest}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public CompartmentRequest getCompartmentRequest()
    {
        return compartmentRequest;
    }

    /**
     * @param compartmentRequest sets the {@link #compartmentRequest}
     */
    public void setCompartmentRequest(CompartmentRequest compartmentRequest)
    {
        this.compartmentRequest = compartmentRequest;
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
    public cz.cesnet.shongo.controller.api.AllocatedCompartment toApi(Domain domain)
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
    }
}
