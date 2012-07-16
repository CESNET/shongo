package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestType;
import org.joda.time.Interval;

/**
 * Request for reservation of resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestSummary extends ComplexType
{
    /**
     * @see ReservationRequest#TYPE
     */
    private ReservationRequestType type;

    /**
     * @see ReservationRequest#NAME
     */
    private String name;

    /**
     * @see ReservationRequest#PURPOSE
     */
    private ReservationRequestPurpose purpose;

    /**
     * @see ReservationRequest#DESCRIPTION
     */
    private String description;

    /**
     * Earliest slot.
     */
    private Interval earliestSlot;

    /**
     * @return {@link #type}
     */
    public ReservationRequestType getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    void setType(ReservationRequestType type)
    {
        this.type = type;
    }

    /**
     * @return {@link #purpose}
     */
    public ReservationRequestPurpose getPurpose()
    {
        return purpose;
    }

    /**
     * @param purpose sets the {@link #purpose}
     */
    void setPurpose(ReservationRequestPurpose purpose)
    {
        this.purpose = purpose;
    }

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #description}
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description sets the {@link #description}
     */
    void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return {@link #earliestSlot}
     */
    public Interval getEarliestSlot()
    {
        return earliestSlot;
    }

    /**
     * @param earliestSlot sets the {@link #earliestSlot}
     */
    void setEarliestSlot(Interval earliestSlot)
    {
        this.earliestSlot = earliestSlot;
    }
}
