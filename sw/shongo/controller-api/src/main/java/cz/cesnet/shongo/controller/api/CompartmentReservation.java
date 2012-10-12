package cz.cesnet.shongo.controller.api;

/**
 * Represents a {@link Reservation} for a {@link CompartmentSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentReservation extends Reservation
{
    /**
     * @see {@link Compartment}
     */
    private Compartment compartment;

    /**
     * @return {@link #compartment}
     */
    public Compartment getCompartment()
    {
        return compartment;
    }

    /**
     * @param compartment sets the {@link #compartment}
     */
    public void setCompartment(Compartment compartment)
    {
        this.compartment = compartment;
    }
}
