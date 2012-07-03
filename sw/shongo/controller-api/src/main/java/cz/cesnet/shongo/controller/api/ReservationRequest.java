package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestType;
import org.joda.time.Period;

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
     * Reservation request identifier
     */
    private String identifier;

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
     * Constructor.
     */
    public ReservationRequest()
    {
    }

    /**
     * @return {@link #identifier}
     */
    public String getIdentifier()
    {
        return identifier;
    }

    /**
     * @param identifier sets the {@link #identifier}
     */
    void setIdentifier(String identifier)
    {
        this.identifier = identifier;
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
     * @param slots sets the {@link #slots}
     */
    public void setSlots(List<DateTimeSlot> slots)
    {
        this.slots = slots;
    }

    /**
     * Add new slot to the {@link #slots}.
     *
     * @param start
     * @param duration
     */
    public void addSlot(Object start, Period duration)
    {
        slots.add(new DateTimeSlot(start, duration));
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
     * @param compartments sets the {@link #compartments}
     */
    public void setCompartments(List<Compartment> compartments)
    {
        this.compartments = compartments;
    }

    /**
     * @return newly added {@link Compartment} to the {@link #compartments}
     */
    public Compartment addCompartment()
    {
        Compartment compartment = new Compartment();
        compartments.add(compartment);
        return compartment;
    }
}
