package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

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

    private static final String VALUE = "value";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(VALUE, value);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        value = dataMap.getString(VALUE, DEFAULT_COLUMN_LENGTH);
    }
}
