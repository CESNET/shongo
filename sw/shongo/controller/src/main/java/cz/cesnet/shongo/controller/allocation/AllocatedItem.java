package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.PersistentObject;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.*;

/**
 * Represents an allocated {@link cz.cesnet.shongo.controller.resource.Resource} in an {@link cz.cesnet.shongo.controller.allocation.AllocatedCompartment}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class AllocatedItem extends PersistentObject
{
    /**
     * {@link cz.cesnet.shongo.controller.allocation.AllocatedCompartment} for which the resource is allocated.
     */
    private AllocatedCompartment allocatedCompartment;

    /**
     * Interval start date/time.
     */
    private DateTime slotStart;

    /**
     * Interval end date/time.
     */
    private DateTime slotEnd;

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
                oldAllocatedCompartment.removeAllocatedItem(this);
            }
            if (allocatedCompartment != null) {
                this.allocatedCompartment = allocatedCompartment;
                this.allocatedCompartment.addAllocatedItem(this);
            }
        }
    }

    /**
     * @return {@link #slotStart}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
    public DateTime getSlotStart()
    {
        return slotStart;
    }

    /**
     * @param slotStart sets the {@link #slotStart}
     */
    public void setSlotStart(DateTime slotStart)
    {
        this.slotStart = slotStart;
    }

    /**
     * @return {@link #slotEnd}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
    public DateTime getSlotEnd()
    {
        return slotEnd;
    }

    /**
     * @param slotEnd sets the {@link #slotEnd}
     */
    public void setSlotEnd(DateTime slotEnd)
    {
        this.slotEnd = slotEnd;
    }

    /**
     * @return slot ({@link #slotStart}, {@link #slotEnd})
     */
    @Transient
    public Interval getSlot()
    {
        return new Interval(slotStart, slotEnd);
    }

    /**
     * @param slot sets the slot
     */
    public void setSlot(Interval slot)
    {
        setSlotStart(slot.getStart());
        setSlotEnd(slot.getEnd());
    }

    /**
     * Sets the slot to new interval created from given {@code start} and {@code end}.
     *
     * @param start
     * @param end
     */
    public void setSlot(DateTime start, DateTime end)
    {
        setSlotStart(start);
        setSlotEnd(end);
    }
}
