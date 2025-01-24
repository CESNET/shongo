package cz.cesnet.shongo.controller.api;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Converter;
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
     * Parent reservation request identifier.
     */
    private String parentReservationRequestId;

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
     * Number of slots in future (except the earliest date/time slot).
     */
    private Integer futureSlotCount;

    /**
     * {@link AllocationState} of the reservation request for the earliest requested date/time slot.
     */
    private AllocationState allocationState;

    /**
     * {@link ExecutableState} of an executable allocated for the reservation request for the earliest requested date/time slot.
     */
    private ExecutableState executableState;

    /**
     * Reused reservation request identifier.
     */
    private String reusedReservationRequestId;

    /**
     * Last allocated reservation id.
     */
    private String lastReservationId;

    /**
     * {@link ExecutableState} of an executable allocated for a reservation request which reused this
     * reservation request and whose slot is active.
     */
    private ExecutableState usageExecutableState;

    /**
     * @see SpecificationType
     */
    private SpecificationType specificationType;

    /**
     * Technologies which are specified.
     */
    private Set<Technology> specificationTechnologies = new HashSet<Technology>();

    /**
     * Specification {@link Resource#getId()}
     */
    private String resourceId;

    /**
     * Specification participant count for the room.
     */
    private Integer roomParticipantCount;

    /**
     * Specification name of the room.
     */
    private String roomName;

    /**
     * Resource tags.
     */
    private List<Tag> resourceTags = new ArrayList<>();

    /**
     * Specifies whether room has recording service.
     */
    private boolean roomHasRecordingService;

    /**
     * Specifies whether room has recordings.
     */
    private boolean roomHasRecordings;

    /**
     * Can this reservation request summary be cached
     */
    private boolean allowCache = true;

    /**
     * Auxiliary data. This data are specified by the {@link Tag}s of {@link Resource} which is requested for reservation.
     */
    private JsonNode auxData;

    /**
     * @return {@link #resourceTags}
     */
    public List<Tag> getResourceTags() {
        return resourceTags;
    }

    /**
     * @param resourceTags sets the {@link #resourceTags}
     */
    public void setResourceTags(List<Tag> resourceTags) {
        this.resourceTags = resourceTags;
    }

    /**
     * @param resourceTag adds tag to {@link #resourceTags}
     */
    public void addResourceTag(Tag resourceTag) {
        this.resourceTags.add(resourceTag);
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
     * @return {@link #futureSlotCount}
     */
    public Integer getFutureSlotCount()
    {
        return futureSlotCount;
    }

    /**
     * @param futureSlotCount sets the {@link #futureSlotCount}
     */
    public void setFutureSlotCount(Integer futureSlotCount)
    {
        this.futureSlotCount = futureSlotCount;
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
     * @return {@link #executableState}
     */
    public ExecutableState getExecutableState()
    {
        return executableState;
    }

    /**
     * @param executableState {@link #executableState}
     */
    public void setExecutableState(ExecutableState executableState)
    {
        this.executableState = executableState;
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
     * @return {@link #lastReservationId}
     */
    public String getLastReservationId()
    {
        return lastReservationId;
    }

    /**
     * @return {@link #lastReservationId}
     */
    public String getAllocatedReservationId()
    {
        if (AllocationState.NOT_ALLOCATED.equals(allocationState)) {
            return null;
        }
        return lastReservationId;
    }

    /**
     * @param reservationId sets the {@link #lastReservationId}
     */
    public void setLastReservationId(String reservationId)
    {
        this.lastReservationId = reservationId;
    }

    /**
     * @return {@link #usageExecutableState}
     */
    public ExecutableState getUsageExecutableState()
    {
        return usageExecutableState;
    }

    /**
     * @param usageExecutableState sets the {@link #usageExecutableState}
     */
    public void setUsageExecutableState(ExecutableState usageExecutableState)
    {
        this.usageExecutableState = usageExecutableState;
    }

    /**
     * @return {@link #specificationType}
     */
    public SpecificationType getSpecificationType()
    {
        return specificationType;
    }

    /**
     * @param specificationType sets the {@link #specificationType}
     */
    public void setSpecificationType(SpecificationType specificationType)
    {
        this.specificationType = specificationType;
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
     * @param technology to be added to the {@link #specificationTechnologies}
     */
    public void addSpecificationTechnology(Technology technology)
    {
        this.specificationTechnologies.add(technology);
    }

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

    /**
     * @return {@link #roomParticipantCount}
     */
    public Integer getRoomParticipantCount()
    {
        return roomParticipantCount;
    }

    /**
     * @param roomParticipantCount sets the {@link #roomParticipantCount}
     */
    public void setRoomParticipantCount(Integer roomParticipantCount)
    {
        this.roomParticipantCount = roomParticipantCount;
    }

    /**
     * @return {@link #roomName}
     */
    public String getRoomName()
    {
        return roomName;
    }

    /**
     * @param roomName sets the {@link #roomName}
     */
    public void setRoomName(String roomName)
    {
        this.roomName = roomName;
    }

    /**
     * @return {@link #roomHasRecordingService}
     */
    public boolean hasRoomRecordingService()
    {
        return roomHasRecordingService;
    }

    /**
     * @param roomHasRecordingService sets the {@link #roomHasRecordingService}
     */
    public void setRoomHasRecordingService(boolean roomHasRecordingService)
    {
        this.roomHasRecordingService = roomHasRecordingService;
    }

    /**
     * @return {@link #roomHasRecordings}
     */
    public boolean hasRoomRecordings()
    {
        return roomHasRecordings;
    }

    /**
     * @param roomHasRecordings sets the {@link #roomHasRecordings}
     */
    public void setRoomHasRecordings(boolean roomHasRecordings)
    {
        this.roomHasRecordings = roomHasRecordings;
    }

    /**
     * @return {@link #allowCache}
     */
    public boolean isAllowCache()
    {
        return allowCache;
    }

    /**
     * @param allowCache sets the {@link #allowCache}
     */
    public void setAllowCache(boolean allowCache)
    {
        this.allowCache = allowCache;
    }

    /**
     * @return {@link #auxData}
     */
    public JsonNode getAuxData()
    {
        return auxData;
    }

    /**
     * @param auxData sets the {@link #auxData}
     */
    public void setAuxData(JsonNode auxData)
    {
        this.auxData = auxData;
    }

    /**
     * @param auxData sets the {@link #auxData}
     */
    public void setAuxData(String auxData)
    {
        this.auxData = Converter.convertToJsonNode(auxData);
    }

    private static final String PARENT_RESERVATION_REQUEST_ID = "parentReservationRequestId";
    private static final String TYPE = "type";
    private static final String DATETIME = "dateTime";
    private static final String USER_ID = "userId";
    private static final String PURPOSE = "purpose";
    private static final String DESCRIPTION = "description";
    private static final String EARLIEST_SLOT = "earliestSlot";
    private static final String FUTURE_SLOT_COUNT = "futureSlotCount";
    private static final String ALLOCATION_STATE = "allocationState";
    private static final String EXECUTABLE_STATE = "executableState";
    private static final String REUSED_RESERVATION_REQUEST_ID = "reusedReservationRequestId";
    private static final String LAST_RESERVATION_ID = "lastReservationId";
    private static final String USAGE_EXECUTABLE_STATE = "usageExecutableState";
    private static final String SPECIFICATION_TYPE = "specificationType";
    private static final String SPECIFICATION_TECHNOLOGIES = "specificationTechnologies";
    private static final String RESOURCE_ID = "resourceId";
    private static final String ROOM_PARTICIPANT_COUNT = "roomParticipantCount";
    private static final String ROOM_NAME = "roomName";
    private static final String ROOM_HAS_RECORDING_SERVICE = "roomHasRecordingService";
    private static final String ROOM_HAS_RECORDINGS = "roomHasRecordings";
    private static final String ALLOW_CACHE = "allowCache";
    private static final String RESOURCE_TAGS = "resourceTags";
    private static final String AUX_DATA = "auxData";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(PARENT_RESERVATION_REQUEST_ID, parentReservationRequestId);
        dataMap.set(TYPE, type);
        dataMap.set(DATETIME, dateTime);
        dataMap.set(USER_ID, userId);
        dataMap.set(PURPOSE, purpose);
        dataMap.set(DESCRIPTION, description);
        dataMap.set(EARLIEST_SLOT, earliestSlot);
        dataMap.set(FUTURE_SLOT_COUNT, futureSlotCount);
        dataMap.set(ALLOCATION_STATE, allocationState);
        dataMap.set(EXECUTABLE_STATE, executableState);
        dataMap.set(REUSED_RESERVATION_REQUEST_ID, reusedReservationRequestId);
        dataMap.set(LAST_RESERVATION_ID, lastReservationId);
        dataMap.set(USAGE_EXECUTABLE_STATE, usageExecutableState);
        dataMap.set(SPECIFICATION_TYPE, specificationType);
        dataMap.set(SPECIFICATION_TECHNOLOGIES, specificationTechnologies);
        dataMap.set(RESOURCE_ID, resourceId);
        dataMap.set(ROOM_PARTICIPANT_COUNT, roomParticipantCount);
        dataMap.set(ROOM_NAME, roomName);
        dataMap.set(ROOM_HAS_RECORDING_SERVICE, roomHasRecordingService);
        dataMap.set(ROOM_HAS_RECORDINGS, roomHasRecordings);
        dataMap.set(ALLOW_CACHE, allowCache);
        dataMap.set(RESOURCE_TAGS, resourceTags);
        dataMap.set(AUX_DATA, auxData);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        parentReservationRequestId = dataMap.getString(PARENT_RESERVATION_REQUEST_ID);
        type = dataMap.getEnum(TYPE, ReservationRequestType.class);
        dateTime = dataMap.getDateTime(DATETIME);
        userId = dataMap.getString(USER_ID);
        purpose = dataMap.getEnum(PURPOSE, ReservationRequestPurpose.class);
        description = dataMap.getString(DESCRIPTION);
        earliestSlot = dataMap.getInterval(EARLIEST_SLOT);
        futureSlotCount = dataMap.getInteger(FUTURE_SLOT_COUNT);
        allocationState = dataMap.getEnum(ALLOCATION_STATE, AllocationState.class);
        executableState = dataMap.getEnum(EXECUTABLE_STATE, ExecutableState.class);
        reusedReservationRequestId = dataMap.getString(REUSED_RESERVATION_REQUEST_ID);
        lastReservationId = dataMap.getString(LAST_RESERVATION_ID);
        usageExecutableState = dataMap.getEnum(USAGE_EXECUTABLE_STATE, ExecutableState.class);
        specificationType = dataMap.getEnum(SPECIFICATION_TYPE, SpecificationType.class);
        specificationTechnologies = dataMap.getSet(SPECIFICATION_TECHNOLOGIES, Technology.class);
        resourceId = dataMap.getString(RESOURCE_ID);
        roomParticipantCount = dataMap.getInteger(ROOM_PARTICIPANT_COUNT);
        roomName = dataMap.getString(ROOM_NAME);
        roomHasRecordingService = dataMap.getBool(ROOM_HAS_RECORDING_SERVICE);
        roomHasRecordings = dataMap.getBool(ROOM_HAS_RECORDINGS);
        allowCache = dataMap.getBool(ALLOW_CACHE);
        resourceTags = dataMap.getList(RESOURCE_TAGS, Tag.class);
        auxData = dataMap.getJsonNode(AUX_DATA);
    }

    /**
     * Type of specification for {@link ReservationRequestSummary}.
     */
    public static enum SpecificationType
    {
        RESOURCE,
        ROOM,
        PERMANENT_ROOM,
        USED_ROOM,
        ALIAS,
        OTHER
    }
}
