package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.*;

/**
 * Summary for all types of {@link AbstractReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestSummary extends IdentifiedComplexType
{
    /**
     * @see ReservationRequestType
     */
    private ReservationRequestType type;

    /**
     * Date/time when the {@link AbstractReservationRequest} was created.
     */
    private DateTime dateTime;

    /**
     * User-id of the user who created the {@link AbstractReservationRequest}.
     */
    private String userId;

    /**
     * @see AbstractReservationRequest#purpose
     */
    private ReservationRequestPurpose purpose;

    /**
     * @see AbstractReservationRequest#description
     */
    private String description;

    /**
     * The earliest requested date/time slot.
     */
    private Interval earliestSlot;

    /**
     * {@link ReservationRequestSummary.State} of the reservation request for the earliest requested date/time slot.
     */
    private State state;

    /**
     * @see cz.cesnet.shongo.controller.api.ReservationRequestSummary.Specification
     */
    private Specification specification;

    /**
     * Technologies.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Provided reservation request identifier.
     */
    private String providedReservationRequestId;

    /**
     * Last allocated reservation id.
     */
    private String lastReservationId;

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
    public void setType(ReservationRequestType type)
    {
        this.type = type;
    }

    /**
     * @return {@link #dateTime}
     */
    public DateTime getDateTime()
    {
        return dateTime;
    }

    /**
     * @param dateTime sets the {@link #dateTime}
     */
    public void setDateTime(DateTime dateTime)
    {
        this.dateTime = dateTime;
    }

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
     * @return {@link #specification}
     */
    public Specification getSpecification()
    {
        return specification;
    }

    /**
     * @param specification sets the {@link #specification}
     */
    public void setSpecification(Specification specification)
    {
        this.specification = specification;
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
     * @return {@link #providedReservationRequestId}
     */
    public String getProvidedReservationRequestId()
    {
        return providedReservationRequestId;
    }

    /**
     * @param providedReservationRequestId sets the {@link #providedReservationRequestId}
     */
    public void setProvidedReservationRequestId(String providedReservationRequestId)
    {
        this.providedReservationRequestId = providedReservationRequestId;
    }

    /**
     * @return {@link #lastReservationId}
     */
    public String getLastReservationId()
    {
        return lastReservationId;
    }

    /**
     * @param reservationId sets the {@link #lastReservationId}
     */
    public void setLastReservationId(String reservationId)
    {
        this.lastReservationId = reservationId;
    }

    private static final String TYPE = "type";
    private static final String DATETIME = "dateTime";
    private static final String USER_ID = "userId";
    private static final String PURPOSE = "purpose";
    private static final String DESCRIPTION = "description";
    private static final String EARLIEST_SLOT = "earliestSlot";
    private static final String STATE = "state";
    private static final String SPECIFICATION = "specification";
    private static final String TECHNOLOGIES = "technologies";
    private static final String PROVIDED_RESERVATION_REQUEST_ID = "providedReservationRequestId";
    private static final String LAST_RESERVATION_ID = "lastReservationId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(TYPE, type);
        dataMap.set(DATETIME, dateTime);
        dataMap.set(USER_ID, userId);
        dataMap.set(PURPOSE, purpose);
        dataMap.set(DESCRIPTION, description);
        dataMap.set(EARLIEST_SLOT, earliestSlot);
        dataMap.set(STATE, state);
        dataMap.set(SPECIFICATION, specification);
        dataMap.set(TECHNOLOGIES, technologies);
        dataMap.set(PROVIDED_RESERVATION_REQUEST_ID, providedReservationRequestId);
        dataMap.set(LAST_RESERVATION_ID, lastReservationId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        type = dataMap.getEnum(TYPE, ReservationRequestType.class);
        dateTime = dataMap.getDateTime(DATETIME);
        userId = dataMap.getString(USER_ID);
        purpose = dataMap.getEnum(PURPOSE, ReservationRequestPurpose.class);
        description = dataMap.getString(DESCRIPTION);
        earliestSlot = dataMap.getInterval(EARLIEST_SLOT);
        state = dataMap.getEnum(STATE, State.class);
        specification = dataMap.getComplexType(SPECIFICATION, Specification.class);
        technologies = dataMap.getSet(TECHNOLOGIES, Technology.class);
        providedReservationRequestId = dataMap.getString(PROVIDED_RESERVATION_REQUEST_ID);
        lastReservationId = dataMap.getString(LAST_RESERVATION_ID);
    }

    /**
     * State of the reservation request which is represented by the {@link ReservationRequestSummary}.
     */
    public static enum State
    {
        /**
         * Reservation request has not been allocated by the scheduler yet.
         */
        NOT_ALLOCATED(false),

        /**
         * Reservation request is allocated by the scheduler but the allocated executable has not been started yet.
         */
        ALLOCATED(true),

        /**
         * Reservation request is allocated by the scheduler and the allocated executable is started.
         */
        STARTED(true),

        /**
         * Reservation request is allocated by the scheduler and the allocated executable has been started and stopped.
         */
        FINISHED(true),

        /**
         * Reservation request cannot be allocated by the scheduler or the starting of executable failed.
         */
        FAILED(false),

        /**
         * Modification of reservation request cannot be allocated by the scheduler
         * but some previous version of reservation request has been allocated and started.
         */
        MODIFICATION_FAILED(false);

        /**
         * Specifies whether reservation request is allocated.
         */
        private final boolean allocated;

        /**
         * Constructor.
         *
         * @param allocated sets the {@link #allocated}
         */
        private State(boolean allocated)
        {
            this.allocated = allocated;
        }

        /**
         * @return {@link #allocated}
         */
        public boolean isAllocated()
        {
            return allocated;
        }
    }

    /**
     * Type of {@link AbstractReservationRequest}.
     */
    public abstract static class Specification extends IdentifiedComplexType
    {
    }

    /**
     * {@link cz.cesnet.shongo.controller.api.ReservationRequestSummary.Specification} that represents a reservation request for a resource.
     */
    public static class ResourceSpecification extends Specification
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

        private static final String RESOURCE_ID = "resourceId";

        @Override
        public DataMap toData()
        {
            DataMap dataMap = super.toData();
            dataMap.set(RESOURCE_ID, resourceId);
            return dataMap;
        }

        @Override
        public void fromData(DataMap dataMap)
        {
            super.fromData(dataMap);
            resourceId = dataMap.getString(RESOURCE_ID);
        }
    }

    /**
     * {@link cz.cesnet.shongo.controller.api.ReservationRequestSummary.Specification} that represents a reservation request for a virtual room.
     */
    public static class RoomSpecification extends Specification
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

        private static final String PARTICIPANT_COUNT = "participantCount";

        @Override
        public DataMap toData()
        {
            DataMap dataMap = super.toData();
            dataMap.set(PARTICIPANT_COUNT, participantCount);
            return dataMap;
        }

        @Override
        public void fromData(DataMap dataMap)
        {
            super.fromData(dataMap);
            participantCount = dataMap.getInteger(PARTICIPANT_COUNT);
        }
    }

    /**
     * {@link cz.cesnet.shongo.controller.api.ReservationRequestSummary.Specification} that represents a reservation request for a {@link Alias}.
     */
    public static class AliasSpecification extends Specification
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
        public AliasType getAliasType()
        {
            return aliasType;
        }

        /**
         * @param aliasType sets the {@link #aliasType}
         */
        public void setAliasType(AliasType aliasType)
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

        private static final String ALIAS_TYPE = "aliasType";
        private static final String VALUE = "value";

        @Override
        public DataMap toData()
        {
            DataMap dataMap = super.toData();
            dataMap.set(ALIAS_TYPE, aliasType);
            dataMap.set(VALUE, value);
            return dataMap;
        }

        @Override
        public void fromData(DataMap dataMap)
        {
            super.fromData(dataMap);
            aliasType = dataMap.getEnum(ALIAS_TYPE, AliasType.class);
            value = dataMap.getString(VALUE);
        }
    }
}
