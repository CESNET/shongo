package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.common.PersistentObject;
import cz.cesnet.shongo.controller.resource.Resource;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;
import org.joda.time.Interval;

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
    private Interval slot;

    /**
     * @return {@link #allocatedCompartment}
     */
    @ManyToOne
    @Access(AccessType.FIELD)
    public AllocatedCompartment getAllocatedCompartment()
    {
        return allocatedCompartment;
    }

    /**
     * @param allocatedCompartment sets the {@link #allocatedCompartment}
     */
    public void setAllocatedCompartment(AllocatedCompartment allocatedCompartment)
    {
        // Manage bidirectional association
        if (allocatedCompartment != this.allocatedCompartment) {
            if (this.allocatedCompartment != null) {
                AllocatedCompartment oldAllocatedCompartment = this.allocatedCompartment;
                this.allocatedCompartment = null;
                oldAllocatedCompartment.removeAllocatedResource(this);
            }
            if (allocatedCompartment != null) {
                this.allocatedCompartment = allocatedCompartment;
                this.allocatedCompartment.addAllocatedResource(this);
            }
        }
    }

    /**
     * @return {@link #resource}
     */
    @OneToOne
    @Access(AccessType.FIELD)
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
    @Columns(columns = {@Column(name = "slot_start"), @Column(name = "slot_end")})
    @Type(type = "Interval")
    public Interval getSlot()
    {
        return slot;
    }

    /**
     * @param slot sets the {@link #slot}
     */
    public void setSlot(Interval slot)
    {
        this.slot = slot;
    }
}
