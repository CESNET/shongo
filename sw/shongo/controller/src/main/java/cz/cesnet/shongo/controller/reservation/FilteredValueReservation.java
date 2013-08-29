package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.resource.value.FilteredValueProvider;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Represents a {@link ValueReservation} which was created by requesting specific value
 * from {@link FilteredValueProvider}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class FilteredValueReservation extends ValueReservation
{
    /**
     * Value which is allocated.
     */
    private String requestedValue;

    /**
     * Constructor.
     */
    public FilteredValueReservation()
    {
    }

    /**
     * Constructor.
     *
     * @param requestedValue sets the {@link #requestedValue}
     */
    public FilteredValueReservation(String requestedValue)
    {
        setRequestedValue(requestedValue);
    }

    /**
     * @return {@link #value}
     */
    @Column
    public String getRequestedValue()
    {
        return requestedValue;
    }

    /**
     * @param value sets the {@link #value}
     */
    public void setRequestedValue(String value)
    {
        this.requestedValue = value;
    }

    @Override
    public String evaluateValue(String value)
    {
        value = value.replace("{requested-value}", (requestedValue != null ? requestedValue : "{value}"));
        return super.evaluateValue(value);
    }
}
