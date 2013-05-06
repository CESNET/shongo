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
    private ValueReservation valueReservation;

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
     * @return {@link #valueReservation}
     */
    public ValueReservation getValueReservation()
    {
        return valueReservation;
    }

    /**
     * @param valueReservation {@link #valueReservation}
     */
    public void setValueReservation(ValueReservation valueReservation)
    {
        this.valueReservation = valueReservation;
    }
}
