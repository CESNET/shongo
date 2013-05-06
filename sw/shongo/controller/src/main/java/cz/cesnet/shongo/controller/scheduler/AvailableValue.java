package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.reservation.ValueReservation;
import cz.cesnet.shongo.controller.resource.value.ValueProvider;

/**
 * Represents an available value in a {@link ValueProvider}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AvailableValue
{
    /**
     * Available value.
     */
    private String value;

    /**
     * Provided {@link ValueReservation} by which the {@link AvailableValue} is already allocated.
     */
    private AvailableReservation<ValueReservation> availableValueReservation;

    /**
     * @return {@link #value}
     */
    public String getValue()
    {
        return value;
    }

    /**
     * @param value sets the {@link #value}
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * @return {@link #availableValueReservation}
     */
    public AvailableReservation<ValueReservation> getAvailableValueReservation()
    {
        return availableValueReservation;
    }

    /**
     * @param availableValueReservation {@link #availableValueReservation}
     */
    public void setAvailableValueReservation(AvailableReservation<ValueReservation> availableValueReservation)
    {
        this.availableValueReservation = availableValueReservation;
    }
}
