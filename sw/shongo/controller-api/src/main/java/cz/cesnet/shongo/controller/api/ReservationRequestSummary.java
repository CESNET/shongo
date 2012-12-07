package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.util.IdentifiedObject;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import org.joda.time.DateTime;
import org.joda.time.Interval;

/**
 * Request for reservation of resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestSummary extends IdentifiedObject
{
    /**
     * Identifier of the owner user.
     */
    private Integer userId;

    /**
     * Date/time when the reservation request was created.
     */
    private DateTime created;

    /**
     * @see Type
     */
    private Type type;

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
     * @see State
     */
    private State state;

    /**
     * Earliest slot.
     */
    private Interval earliestSlot;

    /**
     * @return {@link #userId}
     */
    public Integer getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(Integer userId)
    {
        this.userId = userId;
    }

    /**
     * @return {@link #created}
     */
    public DateTime getCreated()
    {
        return created;
    }

    /**
     * @param created sets the {@link #created}
     */
    public void setCreated(DateTime created)
    {
        this.created = created;
    }

    /**
     * @return {@link #type}
     */
    public Type getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(Type type)
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
    public void setPurpose(ReservationRequestPurpose purpose)
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
    public void setName(String name)
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
    public void setDescription(String description)
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
    public void setEarliestSlot(Interval earliestSlot)
    {
        this.earliestSlot = earliestSlot;
    }

    /**
     * @return {@link #state}
     */
    public State getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(State state)
    {
        this.state = state;
    }

    /**
     * Type of reservation request.
     */
    public static enum Type
    {
        /**
         * Reservation request that can be created by any user.
         */
        NORMAL,

        /**
         * Reservation request that can be created only by owner of resources,
         * and the reservation can request only owned resources.
         */
        PERMANENT
    }

    /**
     * State of reservation request.
     */
    public static enum State
    {
        /**
         * None reservation has been allocated for the request.
         */
        NOT_ALLOCATED,

        /**
         * At least one reservation has been allocated for the request and none has failed.
         */
        ALLOCATED,

        /**
         * At least one reservation has failed to allocate for the request.
         */
        ALLOCATION_FAILED
    }
}
