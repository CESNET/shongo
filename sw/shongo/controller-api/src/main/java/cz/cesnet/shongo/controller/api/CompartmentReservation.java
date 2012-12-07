package cz.cesnet.shongo.controller.api;

/**
 * Represents a {@link Reservation} for a {@link CompartmentSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentReservation extends Reservation
{
    /**
     * @see {@link cz.cesnet.shongo.controller.api.Executable.Compartment}
     */
    private Executable.Compartment compartment;

    /**
     * @return {@link #compartment}
     */
    public Executable.Compartment getCompartment()
    {
        return compartment;
    }

    /**
     * @param compartment sets the {@link #compartment}
     */
    public void setCompartment(Executable.Compartment compartment)
    {
        this.compartment = compartment;
    }
}
