package cz.cesnet.shongo.controller.api;

/**
 * Summary of a reservation
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
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
    private ReservationRequestType type;

    /**
     * Long description
     */
    private String description;

    /**
     * The first future date/time where the reservation takes place
     */
    //private AbsoluteDateTime dateTime;
    public String getId()
    {
        return id;
    }

    public ReservationRequestType getType()
    {
        return type;
    }

    public String getDescription()
    {
        return description;
    }

    /*public AbsoluteDateTime getDateTime()
    {
        return dateTime;
    }*/
}
