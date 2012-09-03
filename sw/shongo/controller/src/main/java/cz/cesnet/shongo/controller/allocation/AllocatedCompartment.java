package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.request.CompartmentRequest;
import cz.cesnet.shongo.controller.resource.Resource;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "allocatedCompartment")
    @Access(AccessType.FIELD)
    public List<AllocatedItem> getAllocatedItems()
    {
        return allocatedItems;
    }

    /**
     * @return list of {@link AllocatedResource}s from the {@link #allocatedItems}
     */
    @Transient
    public List<AllocatedResource> getAllocatedResources()
    {
        List<AllocatedResource> allocatedResources = new ArrayList<AllocatedResource>();
        for (AllocatedItem allocatedItem : allocatedItems) {
            if (allocatedItem instanceof AllocatedResource) {
                allocatedResources.add((AllocatedResource) allocatedItem);
            }
        }
        return allocatedResources;
    }

    /**
     * @param allocatedItem allocated item to be added to the {@link #allocatedItems}
     */
    public void addAllocatedItem(AllocatedItem allocatedItem)
    {
        // Manage bidirectional association
        if (allocatedItems.contains(allocatedItem) == false) {
            allocatedItems.add(allocatedItem);
            allocatedItem.setAllocatedCompartment(this);
        }
    }

    /**
     * @param allocatedItem allocated item to be removed from the {@link #allocatedItems}
     */
    public void removeAllocatedItem(AllocatedItem allocatedItem)
    {
        // Manage bidirectional association
        if (allocatedItems.contains(allocatedItem)) {
            allocatedItems.remove(allocatedItem);
            allocatedItem.setAllocatedCompartment(null);
        }
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

        cz.cesnet.shongo.controller.api.AllocatedCompartment allocatedCompartmentApi =
                new cz.cesnet.shongo.controller.api.AllocatedCompartment();
        allocatedCompartmentApi.setSlot(compartmentRequest.getRequestedSlot());
        for (AllocatedItem allocatedItem : allocatedItems) {
            if (!(allocatedItem instanceof AllocatedResource)) {
                continue;
            }
            AllocatedResource allocatedResource = (AllocatedResource) allocatedItem;
            Resource resource = allocatedResource.getResource();
            if (allocatedResource instanceof AllocatedVirtualRoom) {
                AllocatedVirtualRoom allocatedVirtualRoom = (AllocatedVirtualRoom) allocatedResource;
                cz.cesnet.shongo.controller.api.AllocatedVirtualRoom allocatedVirtualRoomApi =
                        new cz.cesnet.shongo.controller.api.AllocatedVirtualRoom();
                allocatedVirtualRoomApi.setIdentifier(domain.formatIdentifier(resource.getId()));
                allocatedVirtualRoomApi.setName(resource.getName());
                allocatedVirtualRoomApi.setSlot(compartmentRequest.getRequestedSlot());
                allocatedVirtualRoomApi.setPortCount(allocatedVirtualRoom.getPortCount());
                allocatedCompartmentApi.addAllocatedResource(allocatedVirtualRoomApi);
            }
            else {
                cz.cesnet.shongo.controller.api.AllocatedResource allocatedResourceApi =
                        new cz.cesnet.shongo.controller.api.AllocatedResource();
                allocatedResourceApi.setIdentifier(domain.formatIdentifier(resource.getId()));
                allocatedResourceApi.setName(resource.getName());
                allocatedResourceApi.setSlot(compartmentRequest.getRequestedSlot());
                allocatedCompartmentApi.addAllocatedResource(allocatedResourceApi);
            }
        }
        return allocatedCompartmentApi;
    }
}
