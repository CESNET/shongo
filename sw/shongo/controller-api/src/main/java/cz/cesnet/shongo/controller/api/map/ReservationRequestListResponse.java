package cz.cesnet.shongo.controller.api.map;

import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.map.AbstractRawObject;
import org.joda.time.DateTime;

/**
 * Response for {@link ReservationRequestListRequest} containing list of reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestListResponse extends AbstractListResponse<ReservationRequestListResponse.Item>
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
        public static final String ID = "id";

        /**
         * User-id of the user who created the reservation request.
         */
        public static final String USER_ID = "userId";

        /**
         * Date/time when the reservation request was created.
         */
        public static final String CREATED = "created";

        /**
         * Purpose of the reservation request.
         */
        public static final String PURPOSE = "purpose";

        /**
         * Description of the reservation request.
         */
        public static final String DESCRIPTION = "description";

        /**
         * Type of the reservation request.
         */
        public static final String TYPE = "type";

        public String getId()
        {
            return data.getString(ID);
        }

        public void setId(String id)
        {
            data.set(ID, id);
        }

        public String getUserId()
        {
            return data.getString(USER_ID);
        }

        public void setUserId(String userId)
        {
            data.set(USER_ID, userId);
        }

        public DateTime getCreated()
        {
            return data.getDateTime(CREATED);
        }

        public void setCreated(DateTime created)
        {
            data.set(CREATED, created);
        }

        public ReservationRequestPurpose getPurpose()
        {
            return data.getEnum(PURPOSE, ReservationRequestPurpose.class);
        }

        public void setPurpose(ReservationRequestPurpose purpose)
        {
            data.set(PURPOSE, purpose);
        }

        public String getDescription()
        {
            return data.getString(DESCRIPTION);
        }

        public void setDescription(String description)
        {
            data.set(DESCRIPTION, description);
        }

        public AbstractType getType()
        {
            return data.getObject(TYPE, AbstractType.class);
        }

        public void setType(AbstractType type)
        {
            data.set(TYPE, type);
        }
    }

    public static abstract class AbstractType extends AbstractRawObject
    {
    }

    public static class RoomType extends AbstractType
    {
        /**
         * Requested name for the room.
         */
        public static final String NAME = "name";

        /**
         * Requested number of room participants.
         */
        public static final String PARTICIPANT_COUNT = "participantCount";

        public String getName()
        {
            return data.getString(NAME);
        }

        public void setName(String name)
        {
            data.set(NAME, name);
        }

        public Integer getParticipantCount()
        {
            return data.getInteger(PARTICIPANT_COUNT);
        }

        public void setParticipantCount(Integer participantCount)
        {
            data.set(PARTICIPANT_COUNT, participantCount);
        }
    }

    public static class AliasType extends AbstractType
    {
        /**
         * Requested alias type.
         */
        public static final String TYPE = "type";

        /**
         * Requested alias value.
         */
        public static final String VALUE = "value";

        public cz.cesnet.shongo.AliasType getType()
        {
            return data.getEnum(TYPE, cz.cesnet.shongo.AliasType.class);
        }

        public void setType(cz.cesnet.shongo.AliasType type)
        {
            data.set(TYPE, type);
        }

        public String getValue()
        {
            return data.getString(VALUE);
        }

        public void setValue(String value)
        {
            data.set(VALUE, value);
        }
    }
}
