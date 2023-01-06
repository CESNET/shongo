package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.AclEntry;
import cz.cesnet.shongo.controller.api.AllocationState;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;
import org.joda.time.Interval;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link ListRequest} for {@link ListResponse} with reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestListRequest extends SortableListRequest<ReservationRequestListRequest.Sort>
{
    /**
     * Restricts the possible identifiers of reservation requests in resulting {@link ListResponse}.
     */
    private Set<String> reservationRequestIds = new HashSet<String>();

    /**
     * Restricts the {@link ListResponse} to contain only children of reservation request with this identifier.
     */
    private String parentReservationRequestId;

    /**
     * Restricts the {@link ListResponse} to contain only reservation requests which have
     * specification of one of these types.
     */
    private Set<ReservationRequestSummary.SpecificationType> specificationTypes =
            new HashSet<ReservationRequestSummary.SpecificationType>();

    /**
     * Restricts the {@link ListResponse} to contain only reservation requests which have
     * specification containing only these technologies (if a specification contains different technology
     * the reservation request will be filtered out).
     */
    private Set<Technology> specificationTechnologies = new HashSet<Technology>();

    /**
     * Restricts the {@link ListResponse} to contain only reservation requests which have specification
     * targeting resource with one of these identifiers.
     */
    private Set<String> specificationResourceIds = new HashSet<>();

    /**
     * Restricts the {@link ListResponse} to contain only reservation requests which reuse reservation request with
     * this identifier.
     */
    private String reusedReservationRequestId;

    /**
     * Restricts the {@link ListResponse} to contain only reservation requests which are in this {@link AllocationState}.
     */
    private AllocationState allocationState;

    /**
     * Restricts requested interval for resulting reservation requests.
     */
    private Interval interval;

    /**
     * Restrict request by interval only by date (true) or full date-time (false)
     */
    private boolean intervalDateOnly = true;

    /**
     * Restricts resulting reservation requests to contain only those which were created by user with this user-id or
     * the user has {@link AclEntry} for them.
     */
    private String userId;

    /**
     * Restricts resulting reservation requests to contain only those which has configured participant with this user-id.
     */
    private String participantUserId;

    /**
     * Restricts resulting reservation requests to contains this string in text attributes.
     */
    private String search;

    /**
     * Specifies whether deleted reservation request should be also returned.
     */
    private boolean history;

    /**
     * Constructor.
     */
    public ReservationRequestListRequest()
    {
        super(Sort.class);
    }

    /**
     * Construtor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public ReservationRequestListRequest(SecurityToken securityToken)
    {
        super(Sort.class, securityToken);
    }

    /**
     * Constructor.
     *
     * @param securityToken             sets the {@link #securityToken}
     * @param specificationTechnologies sets the {@link #specificationTechnologies}
     */
    public ReservationRequestListRequest(SecurityToken securityToken, Technology[] specificationTechnologies)
    {
        super(Sort.class, securityToken);
        for (Technology technology : specificationTechnologies) {
            this.specificationTechnologies.add(technology);
        }
    }

    /**
     * @return {@link #reservationRequestIds}
     */
    public Set<String> getReservationRequestIds()
    {
        return reservationRequestIds;
    }

    /**
     * @param reservationRequestIds sets the {@link #reservationRequestIds}
     */
    public void setReservationRequestIds(Set<String> reservationRequestIds)
    {
        this.reservationRequestIds = reservationRequestIds;
    }

    /**
     * @param reservationRequestId to be added to the {@link #reservationRequestIds}
     */
    public void addReservationRequestId(String reservationRequestId)
    {
        reservationRequestIds.add(reservationRequestId);
    }

    /**
     * @return {@link #parentReservationRequestId}
     */
    public String getParentReservationRequestId()
    {
        return parentReservationRequestId;
    }

    /**
     * @param parentReservationRequestId sets the {@link #parentReservationRequestId}
     */
    public void setParentReservationRequestId(String parentReservationRequestId)
    {
        this.parentReservationRequestId = parentReservationRequestId;
    }

    /**
     * @return {@link #specificationTechnologies}
     */
    public Set<Technology> getSpecificationTechnologies()
    {
        return specificationTechnologies;
    }

    /**
     * @param specificationTechnologies sets the {@link #specificationTechnologies}
     */
    public void setSpecificationTechnologies(Set<Technology> specificationTechnologies)
    {
        this.specificationTechnologies = specificationTechnologies;
    }

    /**
     * @param specificationTechnology to be added to the {@link #specificationTechnologies}
     */
    public void addSpecificationTechnology(Technology specificationTechnology)
    {
        specificationTechnologies.add(specificationTechnology);
    }

    /**
     * @return {@link #specificationTypes}
     */
    public Set<ReservationRequestSummary.SpecificationType> getSpecificationTypes()
    {
        return specificationTypes;
    }

    /**
     * @param specificationTypes sets the {@link #specificationTypes}
     */
    public void setSpecificationTypes(Set<ReservationRequestSummary.SpecificationType> specificationTypes)
    {
        this.specificationTypes = specificationTypes;
    }

    /**
     * @param specificationType to be added to the {@link #specificationTypes}
     */
    public void addSpecificationType(ReservationRequestSummary.SpecificationType specificationType)
    {
        specificationTypes.add(specificationType);
    }

    /**
     * @return {@link #specificationResourceIds}
     */
    public Set<String> getSpecificationResourceIds()
    {
        return specificationResourceIds;
    }

    /**
     * @param specificationResourceIds sets the {@link #specificationResourceIds}
     */
    public void setSpecificationResourceIds(Set<String> specificationResourceIds)
    {
        this.specificationResourceIds = specificationResourceIds;
    }

    /**
     * @return {@link #reusedReservationRequestId}
     */
    public String getReusedReservationRequestId()
    {
        return reusedReservationRequestId;
    }

    /**
     * @param reusedReservationRequestId sets the {@link #reusedReservationRequestId}
     */
    public void setReusedReservationRequestId(String reusedReservationRequestId)
    {
        this.reusedReservationRequestId = reusedReservationRequestId;
    }

    /**
     * @return {@link #allocationState}
     */
    public AllocationState getAllocationState()
    {
        return allocationState;
    }

    /**
     * @param allocationState sets the {@link #allocationState}
     */
    public void setAllocationState(AllocationState allocationState)
    {
        this.allocationState = allocationState;
    }

    /**
     * @return {@link #interval}
     */
    public Interval getInterval()
    {
        return interval;
    }

    /**
     * @param interval sets the {@link #interval}
     */
    public void setInterval(Interval interval)
    {
        this.interval = interval;
    }

    /**
     * @return {@link #intervalDateOnly}
     */
    public boolean isIntervalDateOnly()
    {
        return intervalDateOnly;
    }

    /**
     * @param intervalDateOnly sets the {@link #intervalDateOnly}
     */
    public void setIntervalDateOnly(boolean intervalDateOnly)
    {
        this.intervalDateOnly = intervalDateOnly;
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
     * @return {@link #participantUserId}
     */
    public String getParticipantUserId()
    {
        return participantUserId;
    }

    /**
     * @param participantUserId sets the {@link #participantUserId}
     */
    public void setParticipantUserId(String participantUserId)
    {
        this.participantUserId = participantUserId;
    }

    /**
     * @return {@link #search}
     */
    public String getSearch()
    {
        return search;
    }

    /**
     * @param search sets the {@link #search}
     */
    public void setSearch(String search)
    {
        this.search = search;
    }

    /**
     * @return {@link #history}
     */
    public boolean isHistory()
    {
        return history;
    }

    /**
     * @param history sets the {@link #history}
     */
    public void setHistory(boolean history)
    {
        this.history = history;
    }

    /**
     * Enumeration of attributes by which the resulting {@link ListResponse} can be sorted.
     */
    public static enum Sort
    {
        ALIAS_ROOM_NAME,
        RESOURCE_ROOM_NAME,
        DATETIME,
        REUSED_RESERVATION_REQUEST,
        ROOM_PARTICIPANT_COUNT,
        SLOT,
        SLOT_NEAREST,
        STATE,
        TECHNOLOGY,
        TYPE,
        USER
    }

    private static final String RESERVATION_REQUEST_IDS = "reservationRequestIds";
    private static final String PARENT_RESERVATION_REQUEST_ID = "parentReservationRequestId";
    private static final String SPECIFICATION_TYPES = "specificationTypes";
    private static final String SPECIFICATION_TECHNOLOGIES = "specificationTechnologies";
    private static final String SPECIFICATION_RESOURCE_IDS = "specificationResourceIds";
    private static final String REUSED_RESERVATION_REQUEST_ID = "reusedReservationRequestId";
    private static final String ALLOCATION_STATE = "allocationState";
    private static final String INTERVAL = "interval";
    private static final String USER_ID = "userId";
    private static final String PARTICIPANT_USER_ID = "participantUserId";
    private static final String SEARCH = "search";
    private static final String HISTORY = "history";
    private static final String INTERVAL_DATE_ONLY = "intervalDateOnly";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESERVATION_REQUEST_IDS, reservationRequestIds);
        dataMap.set(PARENT_RESERVATION_REQUEST_ID, parentReservationRequestId);
        dataMap.set(SPECIFICATION_TYPES, specificationTypes);
        dataMap.set(SPECIFICATION_TECHNOLOGIES, specificationTechnologies);
        dataMap.set(SPECIFICATION_RESOURCE_IDS, specificationResourceIds);
        dataMap.set(REUSED_RESERVATION_REQUEST_ID, reusedReservationRequestId);
        dataMap.set(ALLOCATION_STATE, allocationState);
        dataMap.set(INTERVAL, interval);
        dataMap.set(USER_ID, userId);
        dataMap.set(PARTICIPANT_USER_ID, participantUserId);
        dataMap.set(SEARCH, search);
        dataMap.set(HISTORY, history);
        dataMap.set(INTERVAL_DATE_ONLY, intervalDateOnly);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        reservationRequestIds = dataMap.getSet(RESERVATION_REQUEST_IDS, String.class);
        parentReservationRequestId = dataMap.getString(PARENT_RESERVATION_REQUEST_ID);
        specificationTypes = (Set) dataMap.getSet(SPECIFICATION_TYPES,
                ReservationRequestSummary.SpecificationType.class);
        specificationTechnologies = dataMap.getSet(SPECIFICATION_TECHNOLOGIES, Technology.class);
        specificationResourceIds = dataMap.getSet(SPECIFICATION_RESOURCE_IDS, String.class);
        reusedReservationRequestId = dataMap.getString(REUSED_RESERVATION_REQUEST_ID);
        allocationState = dataMap.getEnum(ALLOCATION_STATE, AllocationState.class);
        interval = dataMap.getInterval(INTERVAL);
        userId = dataMap.getString(USER_ID);
        participantUserId = dataMap.getString(PARTICIPANT_USER_ID);
        search = dataMap.getString(SEARCH);
        history = dataMap.getBool(HISTORY);
        intervalDateOnly = dataMap.getBool(INTERVAL_DATE_ONLY);
    }
}
