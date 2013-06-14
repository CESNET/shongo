package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.util.IdentifiedObject;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.*;

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
     * @see AbstractReservationRequest#PURPOSE
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
     * Technologies.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * List of provided reservation identifiers.
     */
    private List<String> providedReservationIds = new LinkedList<String>();

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
     * @return {@link #technologies}
     */
    public Set<Technology> getTechnologies()
    {
        return technologies;
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies = technologies;
    }

    /**
     * @param technology to be added to the {@link #technologies}
     */
    public void addTechnology(Technology technology)
    {
        this.technologies.add(technology);
    }

    /**
     * @return {@link #providedReservationIds}
     */
    public List<String> getProvidedReservationIds()
    {
        return providedReservationIds;
    }

    /**
     * @param providedReservationIds sets the {@link #providedReservationIds}
     */
    public void setProvidedReservationIds(List<String> providedReservationIds)
    {
        this.providedReservationIds = providedReservationIds;
    }

    /**
     * @param providedReservationId to be added to the {@link #providedReservationIds}
     */
    public void addProvidedReservationId(String providedReservationId)
    {
        providedReservationIds.add(providedReservationId);
    }

    /**
     * Type of {@link AbstractReservationRequest}.
     */
    public abstract static class Type
    {
    }

    /**
     * {@link Type} that represents a reservation request for a resource.
     */
    public static class ResourceType extends Type
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
