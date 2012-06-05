package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.common.PersistentObject;
import cz.cesnet.shongo.controller.reservation.CompartmentRequest;

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
    public Reservation getReservation()
    {
        return reservation;
    }

    /**
     * @param reservation sets the {@link #reservation}
     */
    public void setReservation(Reservation reservation)
    {
        this.reservation = reservation;
    }

    /**
     * @return {@link #compartmentRequest}
     */
    @OneToOne
    public CompartmentRequest getCompartmentRequest()
    {
        return compartmentRequest;
    }

    /**
     * @param compartmentRequest  sets the {@link #compartmentRequest}
     */
    public void setCompartmentRequest(CompartmentRequest compartmentRequest)
    {
        this.compartmentRequest = compartmentRequest;
    }

    /**
     * @return {@link #allocatedResources}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "allocatedCompartment")
    public List<AllocatedResource> getAllocatedResources()
    {
        return allocatedResources;
    }

    /**
     * @param allocatedResources sets the {@link #allocatedResources}
     */
    private void setAllocatedResources(List<AllocatedResource> allocatedResources)
    {
        this.allocatedResources = allocatedResources;
    }

    /**
     * @param allocatedResource allocated resource to be added to the {@link #allocatedResources}
     */
    public void addAllocatedResource(AllocatedResource allocatedResource)
    {
        this.allocatedResources.add(allocatedResource);
    }
}
