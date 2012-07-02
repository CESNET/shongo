package cz.cesnet.shongo.api;

import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestType;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequest extends ComplexType
{
    public ReservationRequestType type;

    public ReservationRequestPurpose purpose;

    public List<DateTimeSlot> slots = new ArrayList<DateTimeSlot>();

    public List<Compartment> compartments = new ArrayList<Compartment>();


    public void addSlot(Object dateTime, Period duration)
    {
        slots.add(DateTimeSlot.create(dateTime, duration));
    }

    public Compartment addCompartment()
    {
        Compartment compartment = new Compartment();
        compartments.add(compartment);
        return compartment;
    }
}
