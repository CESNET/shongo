package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.common.PersistentObject;
import cz.cesnet.shongo.controller.request.CompartmentRequest;

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
     * Resources that are allocated for the {@link #compartmentRequest}.
     */
    private List<AllocatedResource> allocatedResources = new ArrayList<AllocatedResource>();

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
     * @return {@link #allocatedResources}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "allocatedCompartment")
    @Access(AccessType.FIELD)
    public List<AllocatedResource> getAllocatedResources()
    {
        return allocatedResources;
    }

    /**
     * @param allocatedResource allocated resource to be added to the {@link #allocatedResources}
     */
    public void addAllocatedResource(AllocatedResource allocatedResource)
    {
        // Manage bidirectional association
        if (allocatedResources.contains(allocatedResource) == false) {
            allocatedResources.add(allocatedResource);
            allocatedResource.setAllocatedCompartment(this);
        }
    }

    /**
     * @param allocatedResource allocated resource to be removed from the {@link #allocatedResources}
     */
    public void removeAllocatedResource(AllocatedResource allocatedResource)
    {
        // Manage bidirectional association
        if (allocatedResources.contains(allocatedResource)) {
            allocatedResources.remove(allocatedResource);
            allocatedResource.setAllocatedCompartment(null);
        }
    }
}
