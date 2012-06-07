package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.common.AbsoluteDateTimeSlot;
import cz.cesnet.shongo.common.PersistentObject;
import cz.cesnet.shongo.controller.resource.Resource;

import javax.persistence.*;

/**
 * Represents an allocated {@link Resource} in an {@link AllocatedCompartment}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class AllocatedResource extends PersistentObject
{
    /**
     * {@link AllocatedCompartment} for which the resource is allocated.
     */
    private AllocatedCompartment allocatedCompartment;

    /**
     * Resource that is allocated.
     */
    private Resource resource;

    /**
     * Date/time slot for which is the resource allocated.
     */
    private AbsoluteDateTimeSlot slot;

    /**
     * @return {@link #allocatedCompartment}
     */
    @ManyToOne
    public AllocatedCompartment getAllocatedCompartment()
    {
        return allocatedCompartment;
    }

    /**
     * @param allocatedCompartment sets the {@link #allocatedCompartment}
     */
    public void setAllocatedCompartment(AllocatedCompartment allocatedCompartment)
    {
        this.allocatedCompartment = allocatedCompartment;
    }

    /**
     * @return {@link #resource}
     */
    @OneToOne
    public Resource getResource()
    {
        return resource;
    }

    /**
     * @param resource sets the {@link #resource}
     */
    public void setResource(Resource resource)
    {
        this.resource = resource;
    }

    /**
     * @return {@link #slot}
     */
    @OneToOne
    public AbsoluteDateTimeSlot getSlot()
    {
        return slot;
    }

    /**
     * @param slot sets the {@link #slot}
     */
    public void setSlot(AbsoluteDateTimeSlot slot)
    {
        this.slot = slot;
    }
}
