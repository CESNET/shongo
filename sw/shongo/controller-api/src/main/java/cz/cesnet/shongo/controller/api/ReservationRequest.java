package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestType;
import org.joda.time.Period;
import org.omg.PortableServer.ServantLocatorOperations;

import java.util.ArrayList;
import java.util.List;

/**
 * Request for reservation of resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequest extends ComplexType
{


    /**
     * @see ReservationRequestType
     */
    private ReservationRequestType type;

    /**
     * @see ReservationRequestPurpose
     */
    private ReservationRequestPurpose purpose;

    /**
     * List of {@link DateTimeSlot} for which the reservation is requested.
     */
    private List<DateTimeSlot> slots = new ArrayList<DateTimeSlot>();

    /**
     * List of {@link Compartment} which are requested for the reservation.
     */
    private List<Compartment> compartments = new ArrayList<Compartment>();

    /**
     * Attribute names
     */
    public final String TYPE = "type";
    public final String PURPOSE = "purpose";
    public final String SLOTS = "slots";
    public final String COMPARTMENTS = "compartments";

    /**
     * Constructor.
     */
    public ReservationRequest()
    {
    }

    /**
     * Constructor for existing reservation request with given identifier.
     *
     * @param identifier
     */
    public ReservationRequest(String identifier)
    {
    }

    /**
     * @return {@link #type}
     */
    @Required
    public ReservationRequestType getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(ReservationRequestType type)
    {
        this.type = type;

        markPropertyAsFilled(TYPE);
    }

    /**
     * @return {@link #purpose}
     */
    @Required
    public ReservationRequestPurpose getPurpose()
    {
        return purpose;
    }

    /**
     * @param purpose sets the {@link #purpose}
     */
    public void setPurpose(ReservationRequestPurpose purpose)
    {
        this.purpose = purpose;

        markPropertyAsFilled(PURPOSE);
    }

    /**
     * @return {@link #slots}
     */
    @Required
    public List<DateTimeSlot> getSlots()
    {
        return slots;
    }

    /**
     * Add new slot to the {@link #slots}.
     *
     * @param start
     * @param duration
     */
    public void addSlot(Object start, Period duration)
    {
        DateTimeSlot dateTimeSlot = new DateTimeSlot(start, duration);
        slots.add(dateTimeSlot);
        markCollectionItemAsNew(SLOTS, dateTimeSlot);
    }

    /**
     * @param dateTimeSlot slot to be removed from the {@link #slots}
     */
    public void removeSlot(DateTimeSlot dateTimeSlot)
    {
        slots.remove(dateTimeSlot);
        markCollectionItemAsRemoved(SLOTS, dateTimeSlot);
    }

    /**
     * @return {@link #compartments}
     */
    @Required
    public List<Compartment> getCompartments()
    {
        return compartments;
    }

    /**
     * @return newly added {@link Compartment} to the {@link #compartments}
     */
    public Compartment addCompartment()
    {
        Compartment compartment = new Compartment();
        compartments.add(compartment);
        markCollectionItemAsNew(COMPARTMENTS, compartment);
        return compartment;
    }

    /**
     * @param compartment compartment to be removed from the {@link #compartments}
     */
    public void removeCompartment(Compartment compartment)
    {
        compartments.remove(compartment);
        markCollectionItemAsRemoved(COMPARTMENTS, compartment);
    }
}
