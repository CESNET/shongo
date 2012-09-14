package cz.cesnet.shongo.controller.allocationaold;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.Domain;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.*;

/**
 * Represents an allocated item in an {@link AllocatedCompartment}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class AllocatedItem extends PersistentObject
{
    /**
     * {@link AllocatedCompartment} for which the resource is allocated.
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

    /**
     * @return converted capability to API
     */
    public final cz.cesnet.shongo.controller.api.AllocatedItem toApi(Domain domain)
    {
        cz.cesnet.shongo.controller.api.AllocatedItem api = createApi();
        if (api == null) {
            return null;
        }
        toApi(api, domain);
        return api;
    }

    /**
     * @return new instance of API for this object
     */
    protected cz.cesnet.shongo.controller.api.AllocatedItem createApi()
    {
        return new cz.cesnet.shongo.controller.api.AllocatedItem();
    }

    /**
     * @param api to be filled from this object
     */
    protected void toApi(cz.cesnet.shongo.controller.api.AllocatedItem api, Domain domain)
    {
        api.setSlot(getSlot());
    }
}
