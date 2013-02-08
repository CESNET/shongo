package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.util.IdentifiedObject;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import org.joda.time.DateTime;
import org.joda.time.Interval;

/**
 * Summary for all types of {@link AbstractReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestSummary extends IdentifiedObject
{
    /**
     * User-id of the owner user.
     */
    private String userId;

    /**
     * Date/time when the {@link AbstractReservationRequest} was created.
     */
    private DateTime created;

    /**
     * @see NormalReservationRequest#PURPOSE
     */
    private ReservationRequestPurpose purpose;

    /**
     * @see AbstractReservationRequest#DESCRIPTION
     */
    private String description;

    /**
     * @see Type
     */
    private Type type;

    /**
     * Earliest slot.
     */
    private Interval earliestSlot;

    /**
     * @see ReservationRequestState
     */
    private ReservationRequestState state;

    /**
     * @return {@link #userId}
     */
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(String userId)
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
    public ReservationRequestState getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(ReservationRequestState state)
    {
        this.state = state;
    }

    /**
     * Type of {@link AbstractReservationRequest}.
     */
    public abstract static class Type
    {
    }

    /**
     * {@link Type} that represents a reservation request for a resource
     * which can be created only by the owner of the resource.
     */
    public static class PermanentType extends Type
    {
        /**
         * {@link Resource#getId()}
         */
        private String resourceId;

        /**
         * @return {@link #resourceId}
         */
        public String getResourceId()
        {
            return resourceId;
        }

        /**
         * @param resourceId sets the {@link #resourceId}
         */
        public void setResourceId(String resourceId)
        {
            this.resourceId = resourceId;
        }
    }

    /**
     * {@link Type} that represents a reservation request for a virtual room.
     */
    public static class RoomType extends Type
    {
        /**
         * Requested participant count for the room.
         */
        private Integer participantCount;

        /**
         * Requested name for the room.
         */
        private String name;

        /**
         * @return {@link #participantCount}
         */
        public Integer getParticipantCount()
        {
            return participantCount;
        }

        /**
         * @param participantCount sets the {@link #participantCount}
         */
        public void setParticipantCount(Integer participantCount)
        {
            this.participantCount = participantCount;
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
    }

    /**
     * {@link Type} that represents a reservation request for a {@link Alias}.
     */
    public static class AliasType extends Type
    {
        /**
         * Requested {@link cz.cesnet.shongo.AliasType} for the {@link Alias}.
         */
        private cz.cesnet.shongo.AliasType aliasType;

        /**
         * Requested value for the {@link Alias}.
         */
        private String value;

        /**
         * @return {@link #aliasType}
         */
        public cz.cesnet.shongo.AliasType getAliasType()
        {
            return aliasType;
        }

        /**
         * @param aliasType sets the {@link #aliasType}
         */
        public void setAliasType(cz.cesnet.shongo.AliasType aliasType)
        {
            this.aliasType = aliasType;
        }

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
    }

}
