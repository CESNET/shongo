package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.common.api.AbsoluteDateTime;

/**
 * Summary of a reservation
 *
 * @author Martin Srom
 */
public class ReservationSummary
{
    /**
     * Unique identifier
     */
    private String id;

    /**
     * Type of a reservation
     */
    private ReservationType type;

    /**
     * Long description
     */
    private String description;

    /**
     * The first future date/time where the reservation takes place
     */
    private AbsoluteDateTime dateTime;

    public String getId()
    {
        return id;
    }

    public ReservationType getType()
    {
        return type;
    }

    public String getDescription()
    {
        return description;
    }

    public AbsoluteDateTime getDateTime()
    {
        return dateTime;
    }
}
