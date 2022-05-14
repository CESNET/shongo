package cz.cesnet.shongo.controller.rest.models.reservationrequest;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestReusement;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.AclEntryListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.controller.rest.Cache;
import cz.cesnet.shongo.controller.rest.CacheProvider;
import cz.cesnet.shongo.controller.rest.error.UnsupportedApiException;
import cz.cesnet.shongo.controller.rest.models.TechnologyModel;
import cz.cesnet.shongo.controller.rest.models.TimeInterval;
import cz.cesnet.shongo.controller.rest.models.participant.ParticipantModel;
import cz.cesnet.shongo.controller.rest.models.roles.UserRoleModel;
import cz.cesnet.shongo.controller.rest.models.users.SettingsModel;
import cz.cesnet.shongo.util.SlotHelper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.joda.time.*;

import java.util.*;

import static cz.cesnet.shongo.controller.rest.models.TimeInterval.ISO_8601_PATTERN;

/**
 * Model for {@link AbstractReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @author Filip Karnis
 */
@Slf4j
@Data
public class ReservationRequestCreateModel
{

    @JsonIgnore
    private CacheProvider cacheProvider;

    protected String id;

    protected String parentReservationRequestId;

    @JsonProperty("requestType")
    protected ReservationRequestType type;

    protected String description;

    protected ReservationRequestPurpose purpose = ReservationRequestPurpose.USER;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ISO_8601_PATTERN)
    protected DateTime dateTime;

    protected TechnologyModel technology;

    protected DateTimeZone timeZone = DateTimeZone.UTC;

    protected Integer durationCount;

    protected DurationType durationType;

    protected int slotBeforeMinutes;

    protected int slotAfterMinutes;

    protected PeriodicityModel periodicity;

    /**
     * Exclude date from #excludeDates for highligting in user GUI
     */
    protected LocalDate removedReservationDate;

    @JsonProperty("type")
    protected SpecificationType specificationType;

    protected String roomName;

    protected String e164Number;

    protected String roomReservationRequestId;

    protected ReservationRequestSummary permanentRoomReservationRequest;

    protected Integer participantCount;

    protected String roomPin;

    protected String adminPin;

    protected String guestPin;

    protected boolean roomRecorded;

    protected String roomRecordingResourceId;

    protected AdobeConnectPermissions roomAccessMode;

    protected List<UserRoleModel> userRoles = new LinkedList<>();

    protected List<ParticipantModel> roomParticipants = new LinkedList<>();

    protected boolean roomParticipantNotificationEnabled = false;

    protected String roomMeetingName;

    protected String roomMeetingDescription;

    protected Interval collidingInterval;

    protected boolean collidingWithFirstSlot = false;

    protected boolean allowGuests = false;

    @JsonProperty("resource")
    protected String resourceId;

    private TimeInterval slot;

    /**
     * Create new {@link ReservationRequestModel} from scratch.
     */
    public ReservationRequestCreateModel()
    {
        periodicity = new PeriodicityModel();
        periodicity.setType(PeriodicDateTimeSlot.PeriodicityType.NONE);
        periodicity.setPeriodicityCycle(1);
        slot = new TimeInterval();
        slot.setStart(Temporal.roundDateTimeToMinutes(DateTime.now(), 1));
    }

    /**
     * Create new {@link ReservationRequestModel} from scratch.
     */
    public ReservationRequestCreateModel(CacheProvider cacheProvider)
    {
        this();
        this.cacheProvider = cacheProvider;
    }

    /**
     * Create new {@link ReservationRequestModel} from existing {@code reservationRequest}.
     *
     * @param reservationRequest
     */
    public ReservationRequestCreateModel(AbstractReservationRequest reservationRequest, CacheProvider cacheProvider)
    {
        this(cacheProvider);
        fromApi(reservationRequest, cacheProvider);

        // Load permanent room
        if (specificationType.equals(SpecificationType.ROOM_CAPACITY) && cacheProvider != null) {
            loadPermanentRoom(cacheProvider);
        }
    }

    /**
     * @param reservationService
     * @param securityToken
     * @param cache
     * @return list of reservation requests for permanent rooms
     */
    public static List<ReservationRequestSummary> getPermanentRooms(
            ReservationService reservationService,
            SecurityToken securityToken,
            Cache cache)
    {
        ReservationRequestListRequest request = new ReservationRequestListRequest();
        request.setSecurityToken(securityToken);
        request.addSpecificationType(ReservationRequestSummary.SpecificationType.PERMANENT_ROOM);
        List<ReservationRequestSummary> reservationRequests = new LinkedList<>();

        ListResponse<ReservationRequestSummary> response = reservationService.listReservationRequests(request);
        if (response.getItemCount() > 0) {
            Set<String> reservationRequestIds = new HashSet<>();
            for (ReservationRequestSummary reservationRequestSummary : response) {
                reservationRequestIds.add(reservationRequestSummary.getId());
            }
            cache.fetchObjectPermissions(securityToken, reservationRequestIds);

            for (ReservationRequestSummary reservationRequestSummary : response) {
                ExecutableState executableState = reservationRequestSummary.getExecutableState();
                if (executableState == null || (!executableState.isAvailable() && !executableState.equals(
                        ExecutableState.NOT_STARTED))) {
                    continue;
                }
                Set<ObjectPermission> objectPermissions = cache.getObjectPermissions(securityToken,
                        reservationRequestSummary.getId());
                if (!objectPermissions.contains(ObjectPermission.PROVIDE_RESERVATION_REQUEST)) {
                    continue;
                }
                reservationRequests.add(reservationRequestSummary);
            }
        }
        return reservationRequests;
    }

    /**
     * @param reservationRequestId
     * @param reservationService
     * @param securityToken
     * @return list of deletion dependencies for reservation request with given {@code reservationRequestId}
     */
    public static List<ReservationRequestSummary> getDeleteDependencies(
            String reservationRequestId,
            ReservationService reservationService,
            SecurityToken securityToken)
    {
        // List reservation requests which reuse the reservation request to be deleted
        ReservationRequestListRequest reservationRequestListRequest = new ReservationRequestListRequest();
        reservationRequestListRequest.setSecurityToken(securityToken);
        reservationRequestListRequest.setReusedReservationRequestId(reservationRequestId);
        ListResponse<ReservationRequestSummary> reservationRequests =
                reservationService.listReservationRequests(reservationRequestListRequest);
        return reservationRequests.getItems();
    }

    public LocalTime getStart()
    {
        return slot.getStart().toLocalTime();
    }

    public DateTime getRequestStart()
    {
        return slot.getStart();
    }

    public DateTime getEnd()
    {
        return slot.getEnd();
    }

    public void setEnd(DateTime end)
    {
        slot.setEnd(end);
    }

    public void setCollidingInterval(Interval collidingInterval)
    {
        this.collidingInterval = collidingInterval;

        collidingWithFirstSlot = false;
        DateTime start = getRequestStart();
        if (getEnd() == null) {
            switch (this.durationType) {
                case MINUTE:
                    collidingWithFirstSlot = SlotHelper.areIntervalsColliding(start, this.durationCount, 0, 0,
                            collidingInterval);
                    break;
                case HOUR:
                    collidingWithFirstSlot = SlotHelper.areIntervalsColliding(start, 0, this.durationCount, 0,
                            collidingInterval);
                    break;
                case DAY:
                    collidingWithFirstSlot = SlotHelper.areIntervalsColliding(start, 0, 0, this.durationCount,
                            collidingInterval);
                    break;
                default:
                    return;
            }
        }
        else {
            collidingWithFirstSlot = SlotHelper.areIntervalsColliding(start, getEnd(), collidingInterval);
        }
    }

    public void resetCollidingInterval()
    {
        collidingInterval = null;
        collidingWithFirstSlot = false;
    }

    public String getMeetingRoomResourceName()
    {
        if (resourceId != null) {
            ResourceSummary resourceSummary = cacheProvider.getResourceSummary(resourceId);
            if (resourceSummary != null) {
                return resourceSummary.getName();
            }
        }
        return null;
    }

    public String getMeetingRoomResourceDescription()
    {
        ResourceSummary resourceSummary = cacheProvider.getResourceSummary(resourceId);
        if (resourceSummary != null) {
            return resourceSummary.getDescription();
        }
        return null;
    }

    public String getMeetingRoomResourceDomain()
    {
        if (resourceId != null) {
            ResourceSummary resourceSummary = cacheProvider.getResourceSummary(resourceId);
            if (resourceSummary != null) {
                return resourceSummary.getDomainName();
            }
        }
        return null;
    }

    public Period getSlotBefore()
    {
        if (slotBeforeMinutes != 0) {
            return Period.minutes(slotBeforeMinutes);
        }
        else {
            return null;
        }
    }

    public Period getSlotAfter()
    {
        if (slotAfterMinutes != 0) {
            return Period.minutes(slotAfterMinutes);
        }
        else {
            return null;
        }
    }

    public LocalDate getStartDate()
    {
        return slot.getStart().toLocalDate();
    }

    public void setStartDate(LocalDate startDate)
    {
        this.slot.setStart(startDate.toDateTimeAtStartOfDay());
    }

    public void setRoomReservationRequestId(String roomReservationRequestId)
    {
        if (roomReservationRequestId == null
                || !roomReservationRequestId.equals(this.roomReservationRequestId)) {
            this.permanentRoomReservationRequest = null;
        }
        this.roomReservationRequestId = roomReservationRequestId;
    }

    public void setPermanentRoomReservationRequestId(String permanentRoomReservationRequestId,
            List<ReservationRequestSummary> permanentRooms)
    {
        this.roomReservationRequestId = permanentRoomReservationRequestId;

        if (permanentRoomReservationRequestId != null && getStart() != null) {
            for (ReservationRequestSummary permanentRoomSummary : permanentRooms) {
                if (permanentRoomSummary.getId().equals(permanentRoomReservationRequestId)) {
                    Reservation reservation = cacheProvider.getReservation(permanentRoomSummary.getLastReservationId());
                    Interval permanentRoomSlot = reservation.getSlot();
                    DateTime permanentRoomStart = permanentRoomSlot.getStart().plus(getSlotBefore());
                    permanentRoomStart = Temporal.roundDateTimeToMinutes(permanentRoomStart, 1);
                    if (getRequestStart().isBefore(permanentRoomStart)) {
                        setStartDate(permanentRoomStart.toLocalDate());
                    }
                    break;
                }
            }
        }
    }

    public String getRoomResourceName()
    {
        ResourceSummary resourceSummary = cacheProvider.getResourceSummary(resourceId);
        if (resourceSummary != null) {
            return resourceSummary.getName();
        }
        return null;
    }

    public String getRoomRecordingResourceName()
    {
        ResourceSummary resource = cacheProvider.getResourceSummary(roomRecordingResourceId);
        if (resource != null) {
            return resource.getName();
        }
        else {
            return null;
        }
    }

    public void setRoomAccessMode(AdobeConnectPermissions roomAccessMode)
    {
        AdobeConnectPermissions.checkIfUsableByMeetings(roomAccessMode);
        this.roomAccessMode = roomAccessMode;
    }

    public UserRoleModel getUserRole(String userRoleId)
    {
        if (userRoleId == null) {
            return null;
        }
        for (UserRoleModel userRole : userRoles) {
            if (userRoleId.equals(userRole.getId())) {
                return userRole;
            }
        }
        return null;
    }

    public UserRoleModel addUserRole(UserInformation userInformation, ObjectRole objectRole)
    {
        UserRoleModel userRole = new UserRoleModel(userInformation);
        userRole.setObjectId(id);
        userRole.setRole(objectRole);
        userRoles.add(userRole);
        return userRole;
    }

    public void addUserRole(UserRoleModel userRole)
    {
        userRoles.add(userRole);
    }

    public void removeUserRole(UserRoleModel userRole)
    {
        userRoles.remove(userRole);
    }

    public void addRoomParticipant(ParticipantModel participantModel)
    {
        if (ParticipantModel.Type.USER.equals(participantModel.getType())) {
            String userId = participantModel.getUserId();
            for (ParticipantModel existingParticipant : roomParticipants) {
                String existingUserId = existingParticipant.getUserId();
                ParticipantModel.Type existingType = existingParticipant.getType();
                if (existingType.equals(ParticipantModel.Type.USER) && existingUserId.equals(userId)) {
                    ParticipantRole existingRole = existingParticipant.getRole();
                    if (existingRole.compareTo(participantModel.getRole()) >= 0) {
                        log.warn("Skip adding {} because {} already exists.", participantModel, existingParticipant);
                        return;
                    }
                    else {
                        log.warn("Removing {} because {} will be added.", existingParticipant, participantModel);
                        roomParticipants.remove(existingParticipant);
                    }
                    break;
                }
            }
        }
        roomParticipants.add(participantModel);
    }

    public ParticipantModel addRoomParticipant(UserInformation userInformation, ParticipantRole role)
    {
        ParticipantModel participantModel = new ParticipantModel(userInformation);
        participantModel.setNewId();
        participantModel.setRole(role);
        participantModel.setEmail(userInformation.getEmail());
        participantModel.setName(userInformation.getFullName());
        participantModel.setOrganization(userInformation.getOrganization());
        addRoomParticipant(participantModel);
        return participantModel;
    }

    public void clearRoomParticipants()
    {
        roomParticipants.clear();
    }

    /**
     * Load attributes from given {@code specification}.
     *
     * @param specification
     */
    public void fromSpecificationApi(Specification specification, CacheProvider cacheProvider)
    {
        if (specification instanceof RoomSpecification) {
            RoomSpecification roomSpecification = (RoomSpecification) specification;

            for (RoomSetting roomSetting : roomSpecification.getRoomSettings()) {
                if (roomSetting instanceof H323RoomSetting) {
                    H323RoomSetting h323RoomSetting = (H323RoomSetting) roomSetting;
                    if (h323RoomSetting.getPin() != null) {
                        try {
                            String pin = h323RoomSetting.getPin();
                            if (!pin.isEmpty()) {
                                roomPin = String.valueOf(Integer.parseInt(pin));
                            }
                        }
                        catch (NumberFormatException exception) {
                            log.warn("Failed parsing pin", exception);
                        }
                    }
                }
                if (roomSetting instanceof AdobeConnectRoomSetting) {
                    AdobeConnectRoomSetting adobeConnectRoomSetting = (AdobeConnectRoomSetting) roomSetting;
                    roomPin = adobeConnectRoomSetting.getPin();
                    roomAccessMode = adobeConnectRoomSetting.getAccessMode();
                }
                if (roomSetting instanceof PexipRoomSetting) {
                    PexipRoomSetting pexipRoomSetting = (PexipRoomSetting) roomSetting;
                    guestPin = pexipRoomSetting.getGuestPin();
                    adminPin = pexipRoomSetting.getHostPin();
                    allowGuests = pexipRoomSetting.getAllowGuests();
                }
            }
            roomParticipants.clear();
            for (AbstractParticipant participant : roomSpecification.getParticipants()) {
                roomParticipants.add(new ParticipantModel(participant, cacheProvider));
            }

            RoomEstablishment roomEstablishment = roomSpecification.getEstablishment();
            if (roomEstablishment != null) {
                technology = TechnologyModel.find(roomEstablishment.getTechnologies());
                resourceId = roomEstablishment.getResourceId();

                AliasSpecification roomNameAlias =
                        roomEstablishment.getAliasSpecificationByType(AliasType.ROOM_NAME);
                AliasSpecification e164NumberAlias =
                        roomEstablishment.getAliasSpecificationByType(AliasType.H323_E164);
                if (roomNameAlias != null) {
                    roomName = roomNameAlias.getValue();
                }
                if (e164NumberAlias != null) {
                    e164Number = e164NumberAlias.getValue();
                }
            }

            RoomAvailability roomAvailability = roomSpecification.getAvailability();
            if (roomAvailability != null) {
                slotBeforeMinutes = roomAvailability.getSlotMinutesBefore();
                slotAfterMinutes = roomAvailability.getSlotMinutesAfter();
                participantCount = roomAvailability.getParticipantCount();
                roomParticipantNotificationEnabled = roomAvailability.isParticipantNotificationEnabled();
                roomMeetingName = roomAvailability.getMeetingName();
                roomMeetingDescription = roomAvailability.getMeetingDescription();
                for (ExecutableServiceSpecification service : roomAvailability.getServiceSpecifications()) {
                    if (service instanceof RecordingServiceSpecification) {
                        roomRecorded = service.isEnabled();
                        roomRecordingResourceId = service.getResourceId();
                    }
                }
            }

            if (roomEstablishment != null) {
                specificationType = SpecificationType.VIRTUAL_ROOM;
            }
            else {
                specificationType = SpecificationType.ROOM_CAPACITY;
            }
        }
        else if (specification instanceof ResourceSpecification) {
            ResourceSpecification resourceSpecification = (ResourceSpecification) specification;
            resourceId = resourceSpecification.getResourceId();
            ReservationRequestSummary summary = cacheProvider.getAllocatedReservationRequestSummary(this.id);
            specificationType = SpecificationType.fromReservationRequestSummary(summary, true);
        }
        else {
            throw new UnsupportedApiException(specification);
        }
    }

    /**
     * Load attributes from given {@code abstractReservationRequest}.
     *
     * @param abstractReservationRequest from which the attributes should be loaded
     */
    public void fromApi(AbstractReservationRequest abstractReservationRequest, CacheProvider cacheProvider)
    {
        id = abstractReservationRequest.getId();
        type = abstractReservationRequest.getType();
        dateTime = abstractReservationRequest.getDateTime();
        description = abstractReservationRequest.getDescription();

        // Specification
        Specification specification = abstractReservationRequest.getSpecification();
        fromSpecificationApi(specification, cacheProvider);
        if (SpecificationType.ROOM_CAPACITY.equals(specificationType)) {
            roomReservationRequestId = abstractReservationRequest.getReusedReservationRequestId();
        }

        // Date/time slot and periodicity
        Period duration = null;
        if (abstractReservationRequest instanceof cz.cesnet.shongo.controller.api.ReservationRequest) {
            cz.cesnet.shongo.controller.api.ReservationRequest reservationRequest = (cz.cesnet.shongo.controller.api.ReservationRequest) abstractReservationRequest;
            periodicity.setType(PeriodicDateTimeSlot.PeriodicityType.NONE);
            Interval slot = reservationRequest.getSlot();
            this.slot.setStart(slot.getStart());
            this.slot.setEnd(slot.getEnd());
            duration = slot.toPeriod();

            parentReservationRequestId = reservationRequest.getParentReservationRequestId();
        }
        else if (abstractReservationRequest instanceof ReservationRequestSet) {
            if (specificationType.equals(SpecificationType.VIRTUAL_ROOM)) {
                throw new UnsupportedApiException("Periodicity is not allowed for permanent rooms.");
            }

            ReservationRequestSet reservationRequestSet = (ReservationRequestSet) abstractReservationRequest;
            List<Object> slots = reservationRequestSet.getSlots();
            boolean periodicityEndSet = false;

            int index = 0;
            // Multiple slots awailable only for WEEKLY periodicity, check done few lines lower
            periodicity.setPeriodicDaysInWeek(new PeriodicDateTimeSlot.DayOfWeek[slots.size()]);
            // Set slot properties and periodicity
            for (Object slot : slots) {
                if (!(slot instanceof PeriodicDateTimeSlot)) {
                    throw new UnsupportedApiException("Only periodic date/time slots are allowed.");
                }
                PeriodicDateTimeSlot periodicSlot = (PeriodicDateTimeSlot) slot;
                PeriodicDateTimeSlot.PeriodicityType periodicityType =
                        PeriodicDateTimeSlot.PeriodicityType.fromPeriod(periodicSlot.getPeriod());
                int periodicityCycle = PeriodicDateTimeSlot.PeriodicityType.getPeriodCycle(periodicSlot.getPeriod());

                // Allows multiple slots only for WEEKLY
                if (!PeriodicDateTimeSlot.PeriodicityType.WEEKLY.equals(periodicityType) && slots.size() != 1) {
                    throw new UnsupportedApiException(
                            "Multiple periodic date/time slots are allowed only for week period.");
                }
                // Check if all slots have the same periodicity
                if (periodicity.getType() == null) {
                    periodicity.setType(periodicityType);
                    periodicity.setPeriodicityCycle(periodicityCycle);
                    timeZone = periodicSlot.getTimeZone();
                    duration = periodicSlot.getDuration();
                }
                else if (!periodicity.getType()
                        .equals(periodicityType) || periodicity.getPeriodicityCycle() != periodicityCycle
                        || !periodicSlot.getDuration().equals(duration) || !timeZone.equals(
                        periodicSlot.getTimeZone())) {
                    throw new UnsupportedApiException(
                            "Multiple periodic date/time slots with different periodicity are not allowed.");
                }

                int dayIndex = (periodicSlot.getStart().getDayOfWeek() == 7 ? 1 : periodicSlot.getStart()
                        .getDayOfWeek() + 1);
                if (getStartDate() == null || getStartDate().isAfter(periodicSlot.getStart().toLocalDate())) {
                    this.slot.setStart(periodicSlot.getStart().toDateTime(timeZone));
                    this.slot.setEnd(periodicSlot.getStart().plus(duration));
                }
                periodicity.getPeriodicDaysInWeek()[index] = PeriodicDateTimeSlot.DayOfWeek.fromDayIndex(dayIndex);
                index++;

                if (PeriodicDateTimeSlot.PeriodicityType.MONTHLY.equals(periodicityType)
                        && periodicity.getMonthPeriodicityType() == null) {
                    periodicity.setMonthPeriodicityType(periodicSlot.getMonthPeriodicityType());
                    if (PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType.SPECIFIC_DAY.equals(
                            periodicity.getMonthPeriodicityType())) {
                        periodicity.setPeriodicityDayOrder(periodicSlot.getPeriodicityDayOrder());
                        periodicity.setPeriodicityDayInMonth(periodicSlot.getPeriodicityDayInMonth());
                    }
                }

                ReadablePartial slotEnd = periodicSlot.getEnd();
                LocalDate periodicityEnd;
                if (slotEnd == null) {
                    periodicityEnd = null;
                }
                else if (slotEnd instanceof LocalDate) {
                    periodicityEnd = (LocalDate) slotEnd;
                }
                else if (slotEnd instanceof Partial) {
                    Partial partial = (Partial) slotEnd;
                    DateTimeField[] partialFields = partial.getFields();
                    if (!(partialFields.length == 3
                                  && partial.isSupported(DateTimeFieldType.year())
                                  && partial.isSupported(DateTimeFieldType.monthOfYear())
                                  && partial.isSupported(DateTimeFieldType.dayOfMonth()))) {
                        throw new UnsupportedApiException("Slot end %s.", slotEnd);
                    }
                    periodicityEnd = new LocalDate(partial.getValue(0), partial.getValue(1), partial.getValue(2));
                }
                else {
                    throw new UnsupportedApiException("Slot end %s.", slotEnd);
                }

                if (!periodicityEndSet) {
                    periodicity.setPeriodicityEnd(periodicityEnd);
                    periodicityEndSet = true;
                }
                else if ((periodicity.getPeriodicityEnd() == null && periodicity.getPeriodicityEnd() != periodicityEnd)
                        || (periodicity.getPeriodicityEnd() != null && !periodicity.getPeriodicityEnd()
                        .equals(periodicityEnd))) {
                    throw new UnsupportedApiException("Slot end %s is not same for all slots.", slotEnd);
                }

                // Set exclude dates for slot
                if (periodicSlot.getExcludeDates() != null) {
                    if (periodicity.getExcludeDates() == null) {
                        periodicity.setExcludeDates(new LinkedList<>());
                    }
                    periodicity.getExcludeDates().addAll(periodicSlot.getExcludeDates());
                    // Remove duplicates
                    periodicity.setExcludeDates(new ArrayList<>(new HashSet<>(periodicity.getExcludeDates())));
                }
            }
        }
        else {
            throw new UnsupportedApiException(abstractReservationRequest);
        }

        // Room duration
        if (!specificationType.equals(SpecificationType.VIRTUAL_ROOM)) {
            setDuration(duration);
        }
    }

    /**
     * Load {@link #permanentRoomReservationRequest} by given {@code cacheProvider}.
     *
     * @param cacheProvider
     */
    public ReservationRequestSummary loadPermanentRoom(CacheProvider cacheProvider)
    {
        if (StringUtils.isEmpty(roomReservationRequestId)) {
            throw new UnsupportedApiException("Permanent room capacity should have permanent room set.");
        }
        permanentRoomReservationRequest = cacheProvider.getAllocatedReservationRequestSummary(roomReservationRequestId);
        roomName = permanentRoomReservationRequest.getRoomName();
        technology = TechnologyModel.find(permanentRoomReservationRequest.getSpecificationTechnologies());
        addPermanentRoomParticipants();
        return permanentRoomReservationRequest;
    }

    /**
     * Store attributes to {@link Specification}.
     *
     * @return {@link Specification} with stored attributes
     */
    public Specification toSpecificationApi()
    {
        Specification specification;
        switch (specificationType) {
            case VIRTUAL_ROOM: {
                RoomSpecification roomSpecification = new RoomSpecification();
                // Room establishment
                RoomEstablishment roomEstablishment = roomSpecification.createEstablishment();
                roomEstablishment.setTechnologies(technology.getTechnologies());
                AliasSpecification roomNameSpecification = new AliasSpecification();
                roomNameSpecification.addTechnologies(technology.getTechnologies());
                roomNameSpecification.addAliasType(AliasType.ROOM_NAME);
                roomNameSpecification.setValue(roomName);
                roomEstablishment.addAliasSpecification(roomNameSpecification);
                if (technology.equals(TechnologyModel.H323_SIP) || technology.equals(TechnologyModel.PEXIP)) {
                    AliasSpecification e164NumberSpecification = new AliasSpecification(AliasType.H323_E164);
                    if (!Strings.isNullOrEmpty(e164Number)) {
                        e164NumberSpecification.setValue(e164Number);
                    }
                    roomEstablishment.addAliasSpecification(e164NumberSpecification);
                }
                specification = roomSpecification;
                break;
            }
            case ROOM_CAPACITY: {
                RoomSpecification roomSpecification = new RoomSpecification();
                // Room availability
                RoomAvailability roomAvailability = roomSpecification.createAvailability();
                roomAvailability.setParticipantCount(participantCount);
                roomAvailability.setParticipantNotificationEnabled(roomParticipantNotificationEnabled);
                roomAvailability.setMeetingName(roomMeetingName);
                roomAvailability.setMeetingDescription(roomMeetingDescription);
                if (roomRecorded && !technology.equals(TechnologyModel.ADOBE_CONNECT)) {
                    roomAvailability.addServiceSpecification(RecordingServiceSpecification.forResource(
                            Strings.isNullOrEmpty(roomRecordingResourceId) ? null : roomRecordingResourceId, true));
                }
                specification = roomSpecification;
                break;
            }
            case PHYSICAL_RESOURCE:
            case VEHICLE:
            case PARKING_PLACE:
            case MEETING_ROOM: {
                specification = new ResourceSpecification(resourceId);
                break;
            }
            default:
                throw new TodoImplementException(specificationType);
        }

        if (specification instanceof RoomSpecification) {
            RoomSpecification roomSpecification = (RoomSpecification) specification;

            for (ParticipantModel participant : roomParticipants) {
                if (participant.getId() == null) {
                    continue;
                }
                roomSpecification.addParticipant(participant.toApi());
            }

            if (TechnologyModel.PEXIP.equals(technology)) {
                PexipRoomSetting pexipRoomSetting = new PexipRoomSetting();
                if (!allowGuests && !Strings.isNullOrEmpty(guestPin)) {
                    throw new IllegalStateException("Guests must be allowed in order to set a guest pin.");
                }
                pexipRoomSetting.setHostPin(adminPin);
                pexipRoomSetting.setGuestPin(guestPin);
                pexipRoomSetting.setAllowGuests(allowGuests);
                roomSpecification.addRoomSetting(pexipRoomSetting);
            }

            if (TechnologyModel.FREEPBX.equals(technology)) {
                FreePBXRoomSetting freePBXRoomSetting = new FreePBXRoomSetting();
                freePBXRoomSetting.setAdminPin(adminPin);
                freePBXRoomSetting.setUserPin(roomPin);
                roomSpecification.addRoomSetting(freePBXRoomSetting);
            }

            if (TechnologyModel.H323_SIP.equals(technology) && roomPin != null) {
                H323RoomSetting h323RoomSetting = new H323RoomSetting();
                h323RoomSetting.setPin(roomPin);
                roomSpecification.addRoomSetting(h323RoomSetting);
            }
            if (TechnologyModel.ADOBE_CONNECT.equals(technology)) {
                AdobeConnectRoomSetting adobeConnectRoomSetting = new AdobeConnectRoomSetting();
                if (!Strings.isNullOrEmpty(roomPin)) {
                    adobeConnectRoomSetting.setPin(roomPin);
                }
                adobeConnectRoomSetting.setAccessMode(roomAccessMode);
                roomSpecification.addRoomSetting(adobeConnectRoomSetting);
            }
            RoomEstablishment roomEstablishment = roomSpecification.getEstablishment();
            if (roomEstablishment != null) {
                if (!Strings.isNullOrEmpty(resourceId)) {
                    roomEstablishment.setResourceId(resourceId);
                }
            }
            RoomAvailability roomAvailability = roomSpecification.getAvailability();
            if (roomAvailability != null) {
                roomAvailability.setSlotMinutesBefore(slotBeforeMinutes);
                roomAvailability.setSlotMinutesAfter(slotAfterMinutes);
            }
        }
        return specification;
    }

    /**
     * @return requested reservation duration as {@link Period}
     */
    public Period getDuration()
    {
        return new Period(slot.getStart(), slot.getEnd());
    }

    /**
     * @param duration
     */
    public void setDuration(Period duration)
    {
        switch (specificationType) {
            case PHYSICAL_RESOURCE:
            case PARKING_PLACE:
            case VEHICLE:
            case MEETING_ROOM:
            case ROOM_CAPACITY:
                int minutes;
                try {
                    minutes = duration.toStandardMinutes().getMinutes();
                }
                catch (UnsupportedOperationException exception) {
                    throw new UnsupportedApiException(duration.toString(), exception);
                }
                if ((minutes % (60 * 24)) == 0) {
                    durationCount = minutes / (60 * 24);
                    durationType = DurationType.DAY;
                }
                else if ((minutes % 60) == 0) {
                    durationCount = minutes / 60;
                    durationType = DurationType.HOUR;
                }
                else {
                    durationCount = minutes;
                    durationType = DurationType.MINUTE;
                }
                break;
            default:
                throw new TodoImplementException(specificationType);
        }
    }

    /**
     * @return requested first reservation date/time slot as {@link Interval}
     */
    public Interval getFirstSlot()
    {
        PeriodicDateTimeSlot first = getSlots(timeZone).first();
        DateTime start = first.getStart();
        if (timeZone != null) {
            // Use specified time zone
            LocalDateTime localDateTime = start.toLocalDateTime();
            start = localDateTime.toDateTime(timeZone);
        }
        if (specificationType == SpecificationType.VIRTUAL_ROOM) {
            return new Interval(getRequestStart().withTime(0, 0, 0, 0), getDuration());
        }
        return new Interval(start, getDuration());
    }

    public Period getPeriod()
    {
        Period period = null;
        switch (periodicity.getType()) {
            case DAILY:
                period = Period.days(1);
                break;
            case WEEKLY:
                period = Period.weeks(periodicity.getPeriodicityCycle());
                break;
            case MONTHLY:
                period = Period.months(periodicity.getPeriodicityCycle());
                break;
        }
        return period;
    }

    /**
     * @return all calculated slots
     */
    public SortedSet<PeriodicDateTimeSlot> getSlots(DateTimeZone timeZone)
    {
        SortedSet<PeriodicDateTimeSlot> slots = new TreeSet<>();
        if (PeriodicDateTimeSlot.PeriodicityType.NONE.equals(periodicity.getType())) {
            DateTime requestStart = getRequestStart();
            if (SpecificationType.VIRTUAL_ROOM.equals(getSpecificationType())) {
                requestStart = requestStart.withTimeAtStartOfDay();
            }
            PeriodicDateTimeSlot periodicDateTimeSlot =
                    new PeriodicDateTimeSlot(requestStart, getDuration(), Period.ZERO);
            periodicDateTimeSlot.setEnd(getStartDate());
            periodicDateTimeSlot.setTimeZone(getTimeZone());
            slots.add(periodicDateTimeSlot);
        }
        else {
            // Determine period
            Period period = periodicity.getType().toPeriod(periodicity.getPeriodicityCycle());

            if (PeriodicDateTimeSlot.PeriodicityType.WEEKLY.equals(periodicity.getType())) {
                for (PeriodicDateTimeSlot.DayOfWeek day : periodicity.getPeriodicDaysInWeek()) {
                    DateTime nextSlotStart = getRequestStart();
                    int dayIndex = (day.getDayIndex() == 1 ? 7 : day.getDayIndex() - 1);
                    while (nextSlotStart.getDayOfWeek() != dayIndex) {
                        nextSlotStart = nextSlotStart.plusDays(1);
                    }
                    PeriodicDateTimeSlot periodicDateTimeSlot = new PeriodicDateTimeSlot();
                    periodicDateTimeSlot.setStart(nextSlotStart);
                    if (this.timeZone != null) {
                        periodicDateTimeSlot.setTimeZone(this.timeZone);
                    }
                    else if (timeZone != null) {
                        periodicDateTimeSlot.setTimeZone(timeZone);
                    }
                    periodicDateTimeSlot.setDuration(getDuration());
                    periodicDateTimeSlot.setPeriod(period);
                    periodicDateTimeSlot.setEnd(periodicity.getPeriodicityEnd());
                    slots.add(periodicDateTimeSlot);
                }
            }
            else {
                PeriodicDateTimeSlot periodicDateTimeSlot = new PeriodicDateTimeSlot();
                periodicDateTimeSlot.setStart(getFirstSlotStart());
                if (this.timeZone != null) {
                    periodicDateTimeSlot.setTimeZone(this.timeZone);
                }
                else if (timeZone != null) {
                    periodicDateTimeSlot.setTimeZone(timeZone);
                }
                periodicDateTimeSlot.setDuration(getDuration());
                periodicDateTimeSlot.setPeriod(period);
                periodicDateTimeSlot.setEnd(periodicity.getPeriodicityEnd());
                if (PeriodicDateTimeSlot.PeriodicityType.MONTHLY.equals(periodicity.getType())) {
                    if (PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType.SPECIFIC_DAY.equals(
                            periodicity.getMonthPeriodicityType())) {
                        periodicDateTimeSlot.setMonthPeriodicityType(periodicity.getMonthPeriodicityType());
                        periodicDateTimeSlot.setPeriodicityDayOrder(periodicity.getPeriodicityDayOrder());
                        periodicDateTimeSlot.setPeriodicityDayInMonth(periodicity.getPeriodicityDayInMonth());
                    }
                    else {
                        periodicDateTimeSlot.setMonthPeriodicityType(periodicity.getMonthPeriodicityType());
                    }
                }
                slots.add(periodicDateTimeSlot);
            }
        }

        return Collections.unmodifiableSortedSet(slots);
    }

    public DateTime getFirstSlotStart()
    {
        DateTime slotStart = getRequestStart();
        if (PeriodicDateTimeSlot.PeriodicityType.MONTHLY.equals(periodicity.getType())
                && PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType.SPECIFIC_DAY.equals(
                periodicity.getMonthPeriodicityType())) {
            slotStart = getMonthFirstSlotStart(slotStart);
        }
        return slotStart;
    }

    /**
     * Calculate slot start when #periodicityType is MONTHLY and for SPECIFIC_DAY for given #slotStart
     *
     * @param slotStart DateTime from which start
     * @return DateTime for specific date of month
     */
    private DateTime getMonthFirstSlotStart(DateTime slotStart)
    {
        if (!PeriodicDateTimeSlot.PeriodicityType.MONTHLY.equals(periodicity.getType())
                || !PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType.SPECIFIC_DAY.equals(
                periodicity.getMonthPeriodicityType())) {
            throw new IllegalStateException("Periodicity type has to be monthly for a specific day.");
        }
        if (periodicity.getPeriodicityDayInMonth() == null ||
                (
                        periodicity.getPeriodicityDayOrder() != -1 &&
                                (periodicity.getPeriodicityDayOrder() < 1 || periodicity.getPeriodicityDayOrder() > 4)
                )
                || periodicity.getPeriodicityEnd() == null) {
            throw new IllegalStateException("For periodicity type MONTHLY must be set day of month.");
        }

        while (slotStart.getDayOfWeek() != (periodicity.getPeriodicityDayInMonth().getDayIndex() == 1 ? 7
                                                    : periodicity.getPeriodicityDayInMonth().getDayIndex() - 1)) {
            slotStart = slotStart.plusDays(1);
        }
        DateTime monthEnd = slotStart.plusMonths(1).minusDays(slotStart.getDayOfMonth() - 1);
        if (0 < periodicity.getPeriodicityDayOrder() && periodicity.getPeriodicityDayOrder() < 5) {
            while ((slotStart.getDayOfMonth() % 7 == 0
                            ? slotStart.getDayOfMonth() / 7
                            : slotStart.getDayOfMonth() / 7 + 1) != periodicity.getPeriodicityDayOrder()) {
                if (slotStart.plusDays(7).isBefore(monthEnd.plusMonths(1))) {
                    slotStart = slotStart.plusDays(7);
                }
            }
        }
        else if (periodicity.getPeriodicityDayOrder() == -1) {
            while (true) {
                if (!slotStart.plusDays(7).isAfter(monthEnd.minusDays(1))) {
                    slotStart = slotStart.plusDays(7);
                }
                else {
                    break;
                }
            }
        }
        else {
            throw new TodoImplementException();
        }
        return slotStart;
    }

    /**
     * @param participantId
     * @return {@link ParticipantModel} with given {@code participantId}
     */
    public ParticipantModel getParticipant(String participantId)
    {
        ParticipantModel participant = null;
        for (ParticipantModel possibleParticipant : roomParticipants) {
            if (participantId.equals(possibleParticipant.getId())) {
                participant = possibleParticipant;
            }
        }
        if (participant == null) {
            throw new IllegalArgumentException("Participant " + participantId + " doesn't exist.");
        }
        return participant;
    }

    /**
     * @param userId
     * @param role
     * @return true wheter {@link #roomParticipants} contains user participant with given {@code userId} and {@code role}
     */
    public boolean hasUserParticipant(String userId, ParticipantRole role)
    {
        for (ParticipantModel participant : roomParticipants) {
            if (participant.getType().equals(ParticipantModel.Type.USER) && participant.getUserId().equals(userId)
                    && role.equals(participant.getRole())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add new participant.
     *
     * @param participant
     * @param bindingResult
     */
    public boolean createParticipant(ParticipantModel participant, SecurityToken securityToken)
    {
        participant.setNewId();
        addRoomParticipant(participant);
        return true;
    }

    /**
     * Modify existing participant
     *
     * @param participantId
     * @param participant
     * @param bindingResult
     */
    public boolean modifyParticipant(String participantId, ParticipantModel participant, SecurityToken securityToken)
    {
        ParticipantModel oldParticipant = getParticipant(participantId);
        roomParticipants.remove(oldParticipant);
        addRoomParticipant(participant);
        return true;
    }

    /**
     * Delete existing participant.
     *
     * @param participantId
     */
    public void deleteParticipant(String participantId)
    {
        ParticipantModel participant = getParticipant(participantId);
        roomParticipants.remove(participant);
    }


    public LocalDate getFirstFutureSlotStart()
    {
        DateTime slotStart = getRequestStart();
        Period duration = getDuration();

        Period period = getPeriod();
        while (slotStart.plus(duration).isBeforeNow() && period != null) {
            switch (periodicity.getType()) {
                case WEEKLY:
                    if (periodicity.getPeriodicDaysInWeek().length > 1) {
                        Set<Integer> daysOfWeek = new HashSet<>();
                        for (PeriodicDateTimeSlot.DayOfWeek day : periodicity.getPeriodicDaysInWeek()) {
                            daysOfWeek.add(day.getDayIndex() == 1 ? 7 : day.getDayIndex() - 1);
                        }
                        while (!daysOfWeek.contains(slotStart.getDayOfWeek()) || slotStart.plus(duration)
                                .isBeforeNow()) {
                            slotStart = slotStart.plusDays(1);
                        }
                    }
                    else {
                        slotStart = slotStart.plus(period);
                    }
                    break;
                case MONTHLY:
                    if (PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType.SPECIFIC_DAY.equals(
                            periodicity.getMonthPeriodicityType())) {
                        slotStart = getMonthFirstSlotStart(slotStart.plus(period).withDayOfMonth(1));
                        break;
                    }
                    else {
                        slotStart = slotStart.plus(period);
                    }
                    break;
                case DAILY:
                    slotStart = slotStart.plus(period);
                    break;
                default:
                    throw new TodoImplementException("Unsupported periodicity type: " + periodicity.getType());
            }
        }
        return slotStart.toLocalDate();
    }

    /**
     * Store all attributes to {@link AbstractReservationRequest}.
     *
     * @return {@link AbstractReservationRequest} with stored attributes
     */
    public AbstractReservationRequest toApi()
    {
        SortedSet<PeriodicDateTimeSlot> slots = getSlots(DateTimeZone.UTC);
        // Create reservation request
        AbstractReservationRequest abstractReservationRequest;
        if (specificationType == SpecificationType.VIRTUAL_ROOM) {
            cz.cesnet.shongo.controller.api.ReservationRequest reservationRequest = new ReservationRequest();
            PeriodicDateTimeSlot slot = slots.first();
            reservationRequest.setSlot(slot.getStart(), slot.getStart().plus(Duration.standardDays(730)));
            abstractReservationRequest = reservationRequest;
        }
        else if (periodicity.getType() == PeriodicDateTimeSlot.PeriodicityType.NONE) {
            // Create single reservation request
            cz.cesnet.shongo.controller.api.ReservationRequest reservationRequest = new ReservationRequest();
            PeriodicDateTimeSlot slot = slots.first();
            reservationRequest.setSlot(slot.getStart(), slot.getStart().plus(slot.getDuration()));
            abstractReservationRequest = reservationRequest;
        }
        else {
            // Create set of reservation requests
            ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
            reservationRequestSet.addAllSlots(slots);
            if (periodicity.getExcludeDates() != null && !periodicity.getExcludeDates().isEmpty()) {
                for (LocalDate excludeDate : periodicity.getExcludeDates()) {
                    for (PeriodicDateTimeSlot slot : slots) {
                        if (Temporal.dateFitsInterval(slot.getStart(), slot.getEnd(), excludeDate)) {
                            slot.addExcludeDate(excludeDate);
                        }
                    }
                }
            }
            abstractReservationRequest = reservationRequestSet;
        }
        if (!Strings.isNullOrEmpty(id)) {
            abstractReservationRequest.setId(id);
        }
        abstractReservationRequest.setPurpose(purpose);
        abstractReservationRequest.setDescription(description);
        if (specificationType.equals(SpecificationType.VIRTUAL_ROOM)) {
            abstractReservationRequest.setReusement(ReservationRequestReusement.OWNED);
        }
        else if (specificationType.equals(SpecificationType.ROOM_CAPACITY)) {
            abstractReservationRequest.setReusedReservationRequestId(roomReservationRequestId);
        }

        // Create specification
        Specification specification = toSpecificationApi();
        abstractReservationRequest.setSpecification(specification);

        // Set reservation request to be deleted by scheduler if foreign resource is specified
        abstractReservationRequest.setIsSchedulerDeleted(!Strings.isNullOrEmpty(getMeetingRoomResourceDomain()));

        return abstractReservationRequest;
    }

    /**
     * Add all {@link ParticipantModel} from {@link #permanentRoomReservationRequest} to {@link #roomParticipants}
     */
    private void addPermanentRoomParticipants()
    {
        if (permanentRoomReservationRequest == null) {
            throw new IllegalStateException("Permanent room reservation request must not be null");
        }
        String permanentRoomReservationId = permanentRoomReservationRequest.getAllocatedReservationId();
        Reservation permanentRoomReservation = cacheProvider.getReservation(permanentRoomReservationId);
        String permanentRoomId = permanentRoomReservation.getExecutable().getId();
        AbstractRoomExecutable permanentRoom = (AbstractRoomExecutable) cacheProvider.getExecutable(permanentRoomId);
        RoomExecutableParticipantConfiguration permanentRoomParticipants = permanentRoom.getParticipantConfiguration();

        // Remove all participants without identifier (old permanent room participants)
        roomParticipants.removeIf(roomParticipant -> roomParticipant.getId() == null);
        // Add all permanent room participants
        int index = 0;
        for (AbstractParticipant participant : permanentRoomParticipants.getParticipants()) {
            ParticipantModel roomParticipant = new ParticipantModel(participant, cacheProvider);
            roomParticipant.setNullId();
            roomParticipants.add(index++, roomParticipant);
        }
    }

    /**
     * Load user roles into this {@link ReservationRequestModel}.
     *
     * @param securityToken
     * @param authorizationService
     */
    public void loadUserRoles(SecurityToken securityToken, AuthorizationService authorizationService)
    {
        if (id == null) {
            throw new IllegalStateException("Id mustn't be null.");
        }
        // Add user roles
        AclEntryListRequest userRoleRequest = new AclEntryListRequest();
        userRoleRequest.setSecurityToken(securityToken);
        userRoleRequest.addObjectId(id);
        for (AclEntry aclEntry : authorizationService.listAclEntries(userRoleRequest)) {
            addUserRole(new UserRoleModel(aclEntry, cacheProvider));
        }
    }

    /**
     * @return default automatically added {@link ParticipantRole} for owner
     */
    public ParticipantRole getDefaultOwnerParticipantRole()
    {
        if (TechnologyModel.H323_SIP.equals(technology)) {
            return ParticipantRole.PARTICIPANT;
        }
        else {
            return ParticipantRole.ADMINISTRATOR;
        }
    }

    @Override
    public String toString()
    {
        return "ReservationRequestModel{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", dateTime=" + dateTime +
                ", technology=" + technology +
                ", start=" + getStart() +
                ", end=" + getEnd() +
                ", excludeDates=" + periodicity.getExcludeDates() +
                ", roomName='" + roomName + '\'' +
                ", resourceId='" + resourceId + '\'' +
                '}';
    }

    /**
     * Type of duration unit.
     */
    public enum DurationType
    {
        MINUTE,
        HOUR,
        DAY
    }
}
