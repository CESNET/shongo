package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.AllocationState;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link ListRequest} for reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestListRequest extends SortableListRequest<ReservationRequestListRequest.Sort>
{
    private Set<String> reservationRequestIds = new HashSet<String>();

    private String parentReservationRequestId;

    private Set<Technology> technologies = new HashSet<Technology>();

    private Set<ReservationRequestSummary.SpecificationType> specificationTypes =
            new HashSet<ReservationRequestSummary.SpecificationType>();

    private String reusedReservationRequestId;

    private AllocationState allocationState;

    private String roomReusedReservationRequestId;

    public ReservationRequestListRequest()
    {
        super(Sort.class);
    }

    public ReservationRequestListRequest(SecurityToken securityToken)
    {
        super(Sort.class, securityToken);
    }

    public ReservationRequestListRequest(SecurityToken securityToken, Technology[] technologies)
    {
        super(Sort.class, securityToken);
        for (Technology technology : technologies) {
            this.technologies.add(technology);
        }
    }

    public Set<String> getReservationRequestIds()
    {
        return reservationRequestIds;
    }

    public void setReservationRequestIds(Set<String> reservationRequestIds)
    {
        this.reservationRequestIds = reservationRequestIds;
    }

    public void addReservationRequestId(String reservationRequestId)
    {
        reservationRequestIds.add(reservationRequestId);
    }

    public String getParentReservationRequestId()
    {
        return parentReservationRequestId;
    }

    public void setParentReservationRequestId(String parentReservationRequestId)
    {
        this.parentReservationRequestId = parentReservationRequestId;
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
        technologies.add(technology);
    }

    public Set<ReservationRequestSummary.SpecificationType> getSpecificationTypes()
    {
        return specificationTypes;
    }

    public void setSpecificationTypes(Set<ReservationRequestSummary.SpecificationType> specificationTypes)
    {
        this.specificationTypes = specificationTypes;
    }

    public void addSpecificationType(ReservationRequestSummary.SpecificationType specificationType)
    {
        specificationTypes.add(specificationType);
    }

    public String getReusedReservationRequestId()
    {
        return reusedReservationRequestId;
    }

    public void setReusedReservationRequestId(String reusedReservationRequestId)
    {
        this.reusedReservationRequestId = reusedReservationRequestId;
    }

    public AllocationState getAllocationState()
    {
        return allocationState;
    }

    public void setAllocationState(AllocationState allocationState)
    {
        this.allocationState = allocationState;
    }

    public String getRoomReusedReservationRequestId()
    {
        return roomReusedReservationRequestId;
    }

    public void setRoomReusedReservationRequestId(String roomReusedReservationRequestId)
    {
        this.roomReusedReservationRequestId = roomReusedReservationRequestId;
    }

    public static enum Sort
    {
        ALIAS_ROOM_NAME,
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
    private static final String TECHNOLOGIES = "technologies";
    private static final String SPECIFICATION_TYPES = "specificationTypes";
    private static final String REUSED_RESERVATION_REQUEST_ID = "reusedReservationRequestId";
    private static final String ALLOCATION_STATE = "allocationState";
    private static final String ROOM_REUSED_RESERVATION_REQUEST_ID = "roomReusedReservationRequestId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESERVATION_REQUEST_IDS, reservationRequestIds);
        dataMap.set(PARENT_RESERVATION_REQUEST_ID, parentReservationRequestId);
        dataMap.set(TECHNOLOGIES, technologies);
        dataMap.set(SPECIFICATION_TYPES, specificationTypes);
        dataMap.set(REUSED_RESERVATION_REQUEST_ID, reusedReservationRequestId);
        dataMap.set(ALLOCATION_STATE, allocationState);
        dataMap.set(ROOM_REUSED_RESERVATION_REQUEST_ID, roomReusedReservationRequestId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        reservationRequestIds = dataMap.getSet(RESERVATION_REQUEST_IDS, String.class);
        parentReservationRequestId = dataMap.getString(PARENT_RESERVATION_REQUEST_ID);
        technologies = dataMap.getSet(TECHNOLOGIES, Technology.class);
        specificationTypes = (Set) dataMap.getSet(
                SPECIFICATION_TYPES, ReservationRequestSummary.SpecificationType.class);
        reusedReservationRequestId = dataMap.getString(REUSED_RESERVATION_REQUEST_ID);
        allocationState = dataMap.getEnum(ALLOCATION_STATE, AllocationState.class);
        roomReusedReservationRequestId = dataMap.getString(ROOM_REUSED_RESERVATION_REQUEST_ID);
    }
}
