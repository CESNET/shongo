package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.map.AbstractRawObject;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Response for {@link ReservationRequestListRequest} containing list of reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestListResponse extends ListResponse<ReservationRequestListResponse.Item>
{
    /**
     * Constructor.
     */
    public ReservationRequestListResponse()
    {
        super(ReservationRequestListResponse.Item.class);
    }

    /**
     * Represents a single reservation request in the list.
     */
    public static class Item extends AbstractRawObject
    {
        /**
         * Identifier of the reservation request.
         */
        private String id;

        /**
         * User-id of the user who created the reservation request.
         */
        private String userId;

        /**
         * Date/time when the reservation request was created.
         */
        private DateTime created;

        /**
         * Purpose of the reservation request.
         */
        private ReservationRequestPurpose purpose;

        /**
         * Description of the reservation request.
         */
        private String description;

        /**
         * Type of the reservation request.
         */
        private AbstractType type;

        /**
         * Technologies.
         */
        private Set<Technology> technologies = new HashSet<Technology>();

        /**
         * List of provided reservation identifiers.
         */
        private Collection<String> providedReservationIds = new LinkedList<String>();

        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        public String getUserId()
        {
            return userId;
        }

        public void setUserId(String userId)
        {
            this.userId = userId;
        }

        public DateTime getCreated()
        {
            return created;
        }

        public void setCreated(DateTime created)
        {
            this.created = created;
        }

        public ReservationRequestPurpose getPurpose()
        {
            return purpose;
        }

        public void setPurpose(ReservationRequestPurpose purpose)
        {
            this.purpose = purpose;
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }

        public AbstractType getType()
        {
            return type;
        }

        public void setType(AbstractType type)
        {
            this.type = type;
        }

        public Set<Technology> getTechnologies()
        {
            return technologies;
        }

        public void setTechnologies(Set<Technology> technologies)
        {
            this.technologies = technologies;
        }

        public void addTechnology(Technology technology)
        {
            this.technologies.add(technology);
        }

        public Collection<String> getProvidedReservationIds()
        {
            return providedReservationIds;
        }

        public void setProvidedReservationIds(Collection<String> providedReservationIds)
        {
            this.providedReservationIds = providedReservationIds;
        }

        public void addProvidedReservationId(String providedReservationId)
        {
            providedReservationIds.add(providedReservationId);
        }
    }

    public static abstract class AbstractType extends AbstractRawObject
    {
    }

    public static class ResourceType extends AbstractType
    {
        /**
         * Resource-id;
         */
        private String resourceId;

        public String getResourceId()
        {
            return resourceId;
        }

        public void setResourceId(String resourceId)
        {
            this.resourceId = resourceId;
        }
    }

    public static class RoomType extends AbstractType
    {
        /**
         * Requested number of room participants.
         */
        private Integer participantCount;

        public Integer getParticipantCount()
        {
            return participantCount;
        }

        public void setParticipantCount(Integer participantCount)
        {
            this.participantCount = participantCount;
        }
    }

    public static class AliasType extends AbstractType
    {
        /**
         * Requested alias type.
         */
        private cz.cesnet.shongo.AliasType type;

        /**
         * Requested alias value.
         */
        private String value;

        public cz.cesnet.shongo.AliasType getType()
        {
            return type;
        }

        public void setType(cz.cesnet.shongo.AliasType type)
        {
            this.type = type;
        }

        public String getValue()
        {
            return value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }
    }
}
