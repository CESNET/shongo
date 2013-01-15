package cz.cesnet.shongo.controller.api;

/**
 * Represents a {@link Reservation} for a value from {@link ValueProvider}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ValueReservation extends ResourceReservation
{
    /**
     * Value which is allocated.
     */
    private String value;

    /**
     * @return {@link #}
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
}
