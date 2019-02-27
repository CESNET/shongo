package cz.cesnet.shongo.client.web.models;

import com.google.common.base.Strings;
import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.client.web.support.Page;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestReusement;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.api.ReservationRequestType;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.AclEntryListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import cz.cesnet.shongo.util.SlotHelper;
import org.apache.commons.lang.StringUtils;
import org.joda.time.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindingResult;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Model for {@link AbstractReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestModel implements ReportModel.ContextSerializable
{
    private static Logger logger = LoggerFactory.getLogger(ReservationRequestModel.class);

    private CacheProvider cacheProvider;

    protected String id;

    protected String parentReservationRequestId;

    protected ReservationRequestType type;

    protected String description;

    protected ReservationRequestPurpose purpose = ReservationRequestPurpose.USER;

    protected DateTime dateTime;

    protected TechnologyModel technology;

    protected DateTimeZone timeZone;

    protected LocalTime start;

    protected DateTime end;

    protected Integer durationCount;

    protected DurationType durationType;

    protected int slotBeforeMinutes;

    protected int slotAfterMinutes;

    protected List<LocalDate> excludeDates;

    /**
     * Exclude date from #excludeDates for highligting in user GUI
     */
    protected LocalDate removedReservationDate;

    /**
     * Type of the period
     */
    protected PeriodicDateTimeSlot.PeriodicityType periodicityType;

    /**
     * Start of the period
     */
    protected LocalDate startDate;

    /**
     * End of the period
     */
    protected LocalDate periodicityEnd;

    /**
     * Days of the period for weekly period
     */
    protected PeriodicDateTimeSlot.DayOfWeek[] periodicDaysInWeek;

    /**
     * Cycle of the period
     */
    protected int periodicityCycle;

    /**
     * Periodicity parameters for specific day in month (e. g. 2. friday in a month)
     */
    protected PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType monthPeriodicityType;
    protected int periodicityDayOrder;
    protected PeriodicDateTimeSlot.DayOfWeek periodicityDayInMonth;

    protected SpecificationType specificationType;

    protected String roomName;

    protected String e164Number;

    protected String permanentRoomReservationRequestId;

    protected ReservationRequestSummary permanentRoomReservationRequest;

    protected String roomResourceId;

    /**
     * TODO:MR
     * Temporary for meeting rooms,
     */
    protected String meetingRoomResourceId;

    protected Integer roomParticipantCount;

    protected String roomPin;

    protected String adminPin;

    protected String guestPin;

    protected boolean roomRecorded;

    protected String roomRecordingResourceId;

    protected AdobeConnectPermissions roomAccessMode;

    protected List<UserRoleModel> userRoles = new LinkedList<UserRoleModel>();

    protected List<ParticipantModel> roomParticipants = new LinkedList<ParticipantModel>();

    protected boolean roomParticipantNotificationEnabled = false;

    protected String roomMeetingName;

    protected String roomMeetingDescription;

    protected Interval collidingInterval;

    protected boolean collidingWithFirstSlot = false;


    protected boolean allowGuests = false;

    /**
     * Create new {@link ReservationRequestModel} from scratch.
     */
    public ReservationRequestModel(CacheProvider cacheProvider)
    {
        this.cacheProvider = cacheProvider;
        setStart(Temporal.roundDateTimeToMinutes(DateTime.now(), 1).toLocalTime());
        setStartDate(Temporal.roundDateTimeToMinutes(DateTime.now(), 1).toLocalDate());
        setPeriodicityType(PeriodicDateTimeSlot.PeriodicityType.NONE);
        setPeriodicityCycle(1);
    }

    /**
     * Create new {@link ReservationRequestModel} from scratch.
     */
    public ReservationRequestModel(CacheProvider cacheProvider, UserSettingsModel userSettingsModel)
    {
        this.cacheProvider = cacheProvider;
        setStart(Temporal.roundDateTimeToMinutes(DateTime.now(), 1).toLocalTime());
        setStartDate(Temporal.roundDateTimeToMinutes(DateTime.now(), 1).toLocalDate());
        setPeriodicityType(PeriodicDateTimeSlot.PeriodicityType.NONE);
        setPeriodicityCycle(1);
        initByUserSettings(userSettingsModel);
    }

    /**
     * Create new {@link ReservationRequestModel} from existing {@code reservationRequest}.
     *
     * @param reservationRequest
     */
    public ReservationRequestModel(AbstractReservationRequest reservationRequest, CacheProvider cacheProvider)
    {
        this.cacheProvider = cacheProvider;
        fromApi(reservationRequest, cacheProvider);

        // Load permanent room
        if (specificationType.equals(SpecificationType.PERMANENT_ROOM_CAPACITY) && cacheProvider != null) {
            loadPermanentRoom(cacheProvider);
        }
    }

    /**
     * @param userSettingsModel to be inited from
     */
    public void initByUserSettings(UserSettingsModel userSettingsModel)
    {
        if (userSettingsModel.getSlotBefore() != null) {
            setSlotBeforeMinutes(userSettingsModel.getSlotBefore());
        }
        if (userSettingsModel.getSlotAfter() != null) {
            setSlotAfterMinutes(userSettingsModel.getSlotAfter());
        }
    }

    /**
     * @return this as {@link ReservationRequestDetailModel}
     */
    public ReservationRequestDetailModel getDetail()
    {
        if (this instanceof ReservationRequestDetailModel) {
            return (ReservationRequestDetailModel) this;
        }
        return null;
    }

    /**
     * @return this as {@link ReservationRequestModificationModel}
     */
    public ReservationRequestModificationModel getModification()
    {
        if (this instanceof ReservationRequestModificationModel) {
            return (ReservationRequestModificationModel) this;
        }
        return null;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getParentReservationRequestId()
    {
        return parentReservationRequestId;
    }

    public ReservationRequestType getType()
    {
        return type;
    }

    public void setType(ReservationRequestType type)
    {
        this.type = type;
    }

    public DateTime getDateTime()
    {
        return dateTime;
    }

    public String getDescription()
    {
        return description;
    }

    public ReservationRequestPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(ReservationRequestPurpose purpose) {
        this.purpose = purpose;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public TechnologyModel getTechnology()
    {
        return technology;
    }

    public void setTechnology(TechnologyModel technology)
    {
        this.technology = technology;
    }

    public DateTimeZone getTimeZone()
    {
        return timeZone;
    }

    public void setTimeZone(DateTimeZone timeZone)
    {
        this.timeZone = timeZone;
    }

    public LocalTime getStart()
    {
        return start;
    }

    public DateTime getRequestStart() {
        return getStartDate().toDateTime(getStart(), getTimeZone());
    }

    public void setStart(LocalTime start)
    {
        this.start = start;
    }

    public DateTime getEnd()
    {
        return end;
    }

    public void setEnd(DateTime end)
    {
        this.end = end;
    }

    public Integer getDurationCount()
    {
        return durationCount;
    }

    public void setDurationCount(Integer durationCount)
    {
        this.durationCount = durationCount;
    }

    public DurationType getDurationType()
    {
        return durationType;
    }

    public void setDurationType(DurationType durationType)
    {
        this.durationType = durationType;
    }

    public int getSlotBeforeMinutes()
    {
        return slotBeforeMinutes;
    }

    public String getMeetingRoomResourceId() {
        return meetingRoomResourceId;
    }

    public void setMeetingRoomResourceId(String meetingRoomResourceId) {
        this.meetingRoomResourceId = meetingRoomResourceId;
    }

    public PeriodicDateTimeSlot.DayOfWeek[] getPeriodicDaysInWeek() {
        return periodicDaysInWeek;
    }

    public void setPeriodicDaysInWeek(PeriodicDateTimeSlot.DayOfWeek[] periodicDaysInWeek) {
        this.periodicDaysInWeek = periodicDaysInWeek;
    }

    public String getAdminPin()
    {
        return adminPin;
    }

    public void setAdminPin(String adminPin)
    {
        this.adminPin = adminPin;
    }

    public String getGuestPin() {
        return guestPin;
    }

    public void setGuestPin(String guestPin) {
        this.guestPin = guestPin;
    }


    public int getPeriodicityCycle() {
        return periodicityCycle;
    }

    public void setPeriodicityCycle(int periodicityCycle) {
        this.periodicityCycle = periodicityCycle;
    }

    public PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType getMonthPeriodicityType() {
        return monthPeriodicityType;
    }

    public void setMonthPeriodicityType(PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType monthPeriodicityType) {
        this.monthPeriodicityType = monthPeriodicityType;
    }

    public int getPeriodicityDayOrder() {
        return periodicityDayOrder;
    }

    public void setPeriodicityDayOrder(int peridicityDayOrder) {
        if ((1 > peridicityDayOrder || peridicityDayOrder > 4) && (peridicityDayOrder != -1)) {
            throw new IllegalArgumentException("Periodicity day order number must be between 1 to 4 or -1.");
        }
        this.periodicityDayOrder = peridicityDayOrder;
    }

    public PeriodicDateTimeSlot.DayOfWeek getPeriodicityDayInMonth() {
        return periodicityDayInMonth;
    }

    public void setPeriodicityDayInMonth(PeriodicDateTimeSlot.DayOfWeek peridicityDayInMonth) {
        this.periodicityDayInMonth = peridicityDayInMonth;
    }

    public List<LocalDate> getExcludeDates()
    {
        return excludeDates;
    }

    public void setExcludeDates(List<LocalDate> exceptSlots)
    {
        this.excludeDates = exceptSlots;
    }

    public Interval getCollidingInterval() {
        return collidingInterval;
    }

    public void setCollidingInterval(Interval collidingInterval) {
        this.collidingInterval = collidingInterval;

        collidingWithFirstSlot = false;
        DateTime start = getRequestStart();
        if (this.end == null) {
            switch (this.durationType) {
                case MINUTE:
                    collidingWithFirstSlot = SlotHelper.areIntervalsColliding(start,this.durationCount,0,0,collidingInterval);
                    break;
                case HOUR:
                    collidingWithFirstSlot = SlotHelper.areIntervalsColliding(start,0,this.durationCount,0,collidingInterval);
                    break;
                case DAY:
                    collidingWithFirstSlot = SlotHelper.areIntervalsColliding(start,0,0,this.durationCount,collidingInterval);
                    break;
                default:
                    return;
            }
        } else {
            collidingWithFirstSlot = SlotHelper.areIntervalsColliding(start,end,collidingInterval);
        }
    }

    public void resetCollidingInterval() {
        collidingInterval = null;
        collidingWithFirstSlot = false;
    }

    public void setCollidingWithFirstSlot(boolean collidingWithFirstSlot) {
        this.collidingWithFirstSlot = collidingWithFirstSlot;
    }

    public boolean isCollidingWithFirstSlot() {
        return collidingWithFirstSlot;
    }

    public String getMeetingRoomResourceName()
    {
        if (meetingRoomResourceId != null) {
            ResourceSummary resource = cacheProvider.getResourceSummary(meetingRoomResourceId);
            if (resource != null) {
                return resource.getName();
            }
        }
        return null;
    }

    public String getMeetingRoomResourceDescription()
    {
        ResourceSummary resource = cacheProvider.getResourceSummary(meetingRoomResourceId);
        if (resource != null) {
            return resource.getDescription();
        }
        return null;
    }

    public String getMeetingRoomResourceDomain()
    {
        if (meetingRoomResourceId != null) {
            ResourceSummary resource = cacheProvider.getResourceSummary(meetingRoomResourceId);
            if (resource != null) {
                return resource.getDomainName();
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

    public void setSlotBeforeMinutes(int slotBeforeMinutes)
    {
        this.slotBeforeMinutes = slotBeforeMinutes;
    }

    public int getSlotAfterMinutes()
    {
        return slotAfterMinutes;
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

    public void setSlotAfterMinutes(int slotAfterMinutes)
    {
        this.slotAfterMinutes = slotAfterMinutes;
    }

    public PeriodicDateTimeSlot.PeriodicityType getPeriodicityType()
    {
        return periodicityType;
    }

    public void setPeriodicityType(PeriodicDateTimeSlot.PeriodicityType periodicityType)
    {
        this.periodicityType = periodicityType;
    }

    public LocalDate getPeriodicityEnd()
    {
        return periodicityEnd;
    }

    public void setPeriodicityEnd(LocalDate periodicityEnd)
    {
        this.periodicityEnd = periodicityEnd;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public SpecificationType getSpecificationType()
    {
        return specificationType;
    }

    public void setSpecificationType(SpecificationType specificationType)
    {
        this.specificationType = specificationType;
    }

    public String getRoomName()
    {
        return roomName;
    }

    public void setRoomName(String roomName)
    {
        this.roomName = roomName;
    }

    public String getE164Number()
    {
        return e164Number;
    }

    public void setE164Number(String e164Number)
    {
        this.e164Number = e164Number;
    }

    public String getPermanentRoomReservationRequestId()
    {
        return permanentRoomReservationRequestId;
    }

    public void setPermanentRoomReservationRequestId(String permanentRoomReservationRequestId)
    {
        if (permanentRoomReservationRequestId == null
                || !permanentRoomReservationRequestId.equals(this.permanentRoomReservationRequestId)) {
            this.permanentRoomReservationRequest = null;
        }
        this.permanentRoomReservationRequestId = permanentRoomReservationRequestId;
    }

    public void setPermanentRoomReservationRequestId(String permanentRoomReservationRequestId,
            List<ReservationRequestSummary> permanentRooms)
    {
        this.permanentRoomReservationRequestId = permanentRoomReservationRequestId;

        if (permanentRoomReservationRequestId != null && start != null) {
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

    public ReservationRequestSummary getPermanentRoomReservationRequest()
    {
        return permanentRoomReservationRequest;
    }

    public String getRoomResourceId()
    {
        return roomResourceId;
    }

    public String getRoomResourceName()
    {
        ResourceSummary resource = cacheProvider.getResourceSummary(roomResourceId);
        if (resource != null) {
            return resource.getName();
        }
        return null;
    }

    public void setRoomResourceId(String roomResourceId)
    {
        this.roomResourceId = roomResourceId;
    }

    public Integer getRoomParticipantCount()
    {
        return roomParticipantCount;
    }

    public void setRoomParticipantCount(Integer roomParticipantCount)
    {
        this.roomParticipantCount = roomParticipantCount;
    }

    public String getRoomPin()
    {
        return roomPin;
    }

    public void setRoomPin(String roomPin)
    {
        this.roomPin = roomPin;
    }

    public boolean isRoomRecorded()
    {
        return roomRecorded;
    }

    public void setRoomRecorded(boolean roomRecorded)
    {
        this.roomRecorded = roomRecorded;
    }

    public String getRoomRecordingResourceId()
    {
        return roomRecordingResourceId;
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

    public void setRoomRecordingResourceId(String roomRecordingResourceId)
    {
        this.roomRecordingResourceId = roomRecordingResourceId;
    }

    public AdobeConnectPermissions getRoomAccessMode()
    {
        return roomAccessMode;
    }

    public void setRoomAccessMode(AdobeConnectPermissions roomAccessMode)
    {
        AdobeConnectPermissions.checkIfUsableByMeetings(roomAccessMode);
        this.roomAccessMode = roomAccessMode;
    }

    public List<UserRoleModel> getUserRoles()
    {
        return userRoles;
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

    public boolean getAllowGuests() {
        return allowGuests;
    }

    public void setAllowGuests(boolean allowGuests) {
        this.allowGuests = allowGuests;
    }

    public List<? extends ParticipantModel> getRoomParticipants()
    {
        return roomParticipants;
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
                        logger.warn("Skip adding {} because {} already exists.", participantModel, existingParticipant);
                        return;
                    }
                    else {
                        logger.warn("Removing {} because {} will be added.", existingParticipant, participantModel);
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
        ParticipantModel participantModel = new ParticipantModel(userInformation, cacheProvider);
        participantModel.setNewId();
        participantModel.setRole(role);
        participantModel.setEmail(participantModel.getUser().getEmail());
        participantModel.setName(participantModel.getUser().getFullName());
        participantModel.setOrganization(participantModel.getUser().getOrganization());
        addRoomParticipant(participantModel);
        return participantModel;
    }

    public String getRoomMeetingName()
    {
        return roomMeetingName;
    }

    public void setRoomMeetingName(String roomMeetingName)
    {
        this.roomMeetingName = roomMeetingName;
    }

    public String getRoomMeetingDescription()
    {
        return roomMeetingDescription;
    }

    public void setRoomMeetingDescription(String roomMeetingDescription)
    {
        this.roomMeetingDescription = roomMeetingDescription;
    }

    public boolean isRoomParticipantNotificationEnabled()
    {
        return roomParticipantNotificationEnabled;
    }

    public void setRoomParticipantNotificationEnabled(boolean roomParticipantNotificationEnabled)
    {
        this.roomParticipantNotificationEnabled = roomParticipantNotificationEnabled;
    }

    public LocalDate getRemovedReservationDate()
    {
        return removedReservationDate;
    }

    public void setRemovedReservationDate(LocalDate removedReservationDate)
    {
        this.removedReservationDate = removedReservationDate;
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
                            logger.warn("Failed parsing pin", exception);
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
                roomResourceId = roomEstablishment.getResourceId();

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
                roomParticipantCount = roomAvailability.getParticipantCount();
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
                if (roomAvailability != null) {
                    specificationType = SpecificationType.ADHOC_ROOM;
                }
                else {
                    specificationType = SpecificationType.PERMANENT_ROOM;
                }
            }
            else {
                specificationType = SpecificationType.PERMANENT_ROOM_CAPACITY;
            }
        } else if (specification instanceof ResourceSpecification) {
            ResourceSpecification resourceSpecification = (ResourceSpecification) specification;
            meetingRoomResourceId = resourceSpecification.getResourceId();
            specificationType = SpecificationType.MEETING_ROOM;
        } else {
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
        if (SpecificationType.PERMANENT_ROOM_CAPACITY.equals(specificationType)) {
            permanentRoomReservationRequestId = abstractReservationRequest.getReusedReservationRequestId();
        }

        // Date/time slot and periodicity
        Period duration = null;
        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;
            periodicityType = PeriodicDateTimeSlot.PeriodicityType.NONE;
            Interval slot = reservationRequest.getSlot();
            start = slot.getStart().toLocalTime();
            startDate = slot.getStart().toLocalDate();
            end = slot.getEnd();
            duration = slot.toPeriod();

            parentReservationRequestId = reservationRequest.getParentReservationRequestId();
        }
        else if (abstractReservationRequest instanceof ReservationRequestSet) {
            if (specificationType.equals(SpecificationType.PERMANENT_ROOM)) {
                throw new UnsupportedApiException("Periodicity is not allowed for permanent rooms.");
            }

            ReservationRequestSet reservationRequestSet = (ReservationRequestSet) abstractReservationRequest;
            List<Object> slots = reservationRequestSet.getSlots();
            boolean periodicityEndSet = false;

            int index = 0;
            // Multiple slots awailable only for WEEKLY periodicity, check done few lines lower
            periodicDaysInWeek = new PeriodicDateTimeSlot.DayOfWeek[slots.size()];
            // Set slot properties and periodicity
            for (Object slot : slots) {
                if (!(slot instanceof PeriodicDateTimeSlot)) {
                    throw new UnsupportedApiException("Only periodic date/time slots are allowed.");
                }
                PeriodicDateTimeSlot periodicSlot = (PeriodicDateTimeSlot) slot;
                PeriodicDateTimeSlot.PeriodicityType periodicityType= PeriodicDateTimeSlot.PeriodicityType.fromPeriod(periodicSlot.getPeriod());
                int periodicityCycle = PeriodicDateTimeSlot.PeriodicityType.getPeriodCycle(periodicSlot.getPeriod());

                // Allows multiple slots only for WEEKLY
                if (!PeriodicDateTimeSlot.PeriodicityType.WEEKLY.equals(periodicityType) && slots.size() != 1) {
                    throw new UnsupportedApiException("Multiple periodic date/time slots are allowed only for week period.");
                }
                // Check if all slots have the same periodicity
                if (this.periodicityType == null) {
                    this.periodicityType = periodicityType;
                    this.periodicityCycle = periodicityCycle;
                    timeZone = periodicSlot.getTimeZone();
                    duration = periodicSlot.getDuration();
                }
                else if (!this.periodicityType.equals(periodicityType) || this.periodicityCycle != periodicityCycle
                        || !periodicSlot.getDuration().equals(duration) || !timeZone.equals(periodicSlot.getTimeZone())) {
                    throw new UnsupportedApiException("Multiple periodic date/time slots with different periodicity are not allowed.");
                }

                int dayIndex = (periodicSlot.getStart().getDayOfWeek() == 7 ? 1  : periodicSlot.getStart().getDayOfWeek() + 1);
                if (startDate == null || startDate.isAfter(periodicSlot.getStart().toLocalDate())) {
                    start = periodicSlot.getStart().toDateTime(timeZone).toLocalTime();
                    startDate = periodicSlot.getStart().toLocalDate();
                    end = periodicSlot.getStart().plus(duration);
                }
                periodicDaysInWeek[index] = PeriodicDateTimeSlot.DayOfWeek.fromDayIndex(dayIndex);
                index++;

                if (PeriodicDateTimeSlot.PeriodicityType.MONTHLY.equals(periodicityType) && monthPeriodicityType == null) {
                    monthPeriodicityType = periodicSlot.getMonthPeriodicityType();
                    if (PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType.SPECIFIC_DAY.equals(monthPeriodicityType)) {
                        periodicityDayOrder = periodicSlot.getPeriodicityDayOrder();
                        periodicityDayInMonth = periodicSlot.getPeriodicityDayInMonth();
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
                    this.periodicityEnd = periodicityEnd;
                    periodicityEndSet = true;
                }
                else if ((this.periodicityEnd == null && this.periodicityEnd != periodicityEnd)
                        || (this.periodicityEnd != null && !this.periodicityEnd.equals(periodicityEnd))) {
                    throw new UnsupportedApiException("Slot end %s is not same for all slots.", slotEnd);
                }

                // Set exclude dates for slot
                if (periodicSlot.getExcludeDates() != null) {
                    if (this.excludeDates == null) {
                        this.excludeDates = new LinkedList<>();
                    }
                    this.excludeDates.addAll(periodicSlot.getExcludeDates());
                    // Remove duplicates
                    this.excludeDates = new ArrayList<>(new HashSet<>(excludeDates));
                }
            }
        }
        else {
            throw new UnsupportedApiException(abstractReservationRequest);
        }

        // Room duration
        if (!specificationType.equals(SpecificationType.PERMANENT_ROOM)) {
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
        if (StringUtils.isEmpty(permanentRoomReservationRequestId)) {
            throw new UnsupportedApiException("Permanent room capacity should have permanent room set.");
        }
        permanentRoomReservationRequest =
                cacheProvider.getAllocatedReservationRequestSummary(permanentRoomReservationRequestId);
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
            case ADHOC_ROOM: {
                RoomSpecification roomSpecification = new RoomSpecification();
                // Room establishment
                RoomEstablishment roomEstablishment = roomSpecification.createEstablishment();
                roomEstablishment.setTechnologies(technology.getTechnologies());
                // Room availability
                RoomAvailability roomAvailability = roomSpecification.createAvailability();
                roomAvailability.setParticipantCount(roomParticipantCount);
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
            case PERMANENT_ROOM: {
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
            case PERMANENT_ROOM_CAPACITY: {
                RoomSpecification roomSpecification = new RoomSpecification();
                // Room availability
                RoomAvailability roomAvailability = roomSpecification.createAvailability();
                roomAvailability.setParticipantCount(roomParticipantCount);
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
            case MEETING_ROOM: {
                specification = new ResourceSpecification(meetingRoomResourceId);
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
                if (!Strings.isNullOrEmpty(roomResourceId)) {
                    roomEstablishment.setResourceId(roomResourceId);
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
        switch (specificationType) {
            case PERMANENT_ROOM:
                if (end == null) {
                    throw new IllegalStateException("Slot end must be not empty for alias.");
                }
                return new Period(startDate.toDateTimeAtStartOfDay(timeZone), end.withZone(timeZone).withTime(23, 59, 59, 0));
            case MEETING_ROOM:
            case ADHOC_ROOM:
            case PERMANENT_ROOM_CAPACITY:
                if (durationCount == null || durationType == null) {
                    if (end != null) {
                        return new Period(startDate.toDateTime(start,timeZone), end);
                    }
                    else {
                        throw new IllegalStateException("Slot duration should be not empty.");
                    }
                }
                switch (durationType) {
                    case MINUTE:
                        return Period.minutes(durationCount);
                    case HOUR:
                        return Period.hours(durationCount);
                    case DAY:
                        return Period.days(durationCount);
                    default:
                        throw new TodoImplementException(durationType);
                }
            default:
                throw new TodoImplementException("Reservation request duration.");
        }
    }

    /**
     * @param duration
     */
    public void setDuration(Period duration)
    {
        switch (specificationType) {
            case MEETING_ROOM:
            case ADHOC_ROOM:
            case PERMANENT_ROOM_CAPACITY:
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
//        DateTime start = this.start;
        if (timeZone != null) {
            // Use specified time zone
            LocalDateTime localDateTime = start.toLocalDateTime();
            start = localDateTime.toDateTime(timeZone);
        }
        switch (specificationType) {
            case PERMANENT_ROOM:
                return new Interval(getRequestStart().withTime(0, 0, 0, 0), getDuration());
            default:
                return new Interval(start, getDuration());
        }
    }

    public Period getPeriod()
    {
        Period period = null;
        switch (periodicityType) {
            case DAILY:
                period = Period.days(1);
                break;
            case WEEKLY:
                period = Period.weeks(periodicityCycle);
                break;
            case MONTHLY:
                period = Period.months(periodicityCycle);
                break;
        }
        return period;
    }

    /**
     *
     * @return all calculated slots
     */
    public SortedSet<PeriodicDateTimeSlot> getSlots(DateTimeZone timeZone) {
        SortedSet<PeriodicDateTimeSlot> slots = new TreeSet<PeriodicDateTimeSlot>();
        if (PeriodicDateTimeSlot.PeriodicityType.NONE.equals(periodicityType)) {
            DateTime requestStart = getRequestStart();
            if (SpecificationType.PERMANENT_ROOM.equals(getSpecificationType())) {
                requestStart = requestStart.withTimeAtStartOfDay();
            }
            PeriodicDateTimeSlot periodicDateTimeSlot = new PeriodicDateTimeSlot(requestStart, getDuration(), Period.ZERO);
            periodicDateTimeSlot.setEnd(getStartDate());
            periodicDateTimeSlot.setTimeZone(getTimeZone());
            slots.add(periodicDateTimeSlot);
        } else {
            // Determine period
            Period period = periodicityType.toPeriod(periodicityCycle);

            if (PeriodicDateTimeSlot.PeriodicityType.WEEKLY.equals(periodicityType)) {
                for (PeriodicDateTimeSlot.DayOfWeek day : periodicDaysInWeek) {
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
                    periodicDateTimeSlot.setEnd(periodicityEnd);
                    slots.add(periodicDateTimeSlot);
                }
            } else {
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
                periodicDateTimeSlot.setEnd(periodicityEnd);
                if (PeriodicDateTimeSlot.PeriodicityType.MONTHLY.equals(periodicityType)) {
                    if (PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType.SPECIFIC_DAY.equals(monthPeriodicityType)) {
                        periodicDateTimeSlot.setMonthPeriodicityType(monthPeriodicityType);
                        periodicDateTimeSlot.setPeriodicityDayOrder(periodicityDayOrder);
                        periodicDateTimeSlot.setPeriodicityDayInMonth(periodicityDayInMonth);
                    } else {
                        periodicDateTimeSlot.setMonthPeriodicityType(monthPeriodicityType);
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
        if (PeriodicDateTimeSlot.PeriodicityType.MONTHLY.equals(periodicityType)
                && PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType.SPECIFIC_DAY.equals(monthPeriodicityType)) {
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
        if (!PeriodicDateTimeSlot.PeriodicityType.MONTHLY.equals(periodicityType)
                || !PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType.SPECIFIC_DAY.equals(monthPeriodicityType)) {
            throw new IllegalStateException("Periodicity type has to be monthly for a specific day.");
        }
        if (periodicityDayInMonth == null || (periodicityDayOrder != -1 && (periodicityDayOrder < 1 || periodicityDayOrder > 4)) || periodicityEnd == null) {
            throw new IllegalStateException("For periodicity type MONTHLY must be set day of month.");
        }

        while (slotStart.getDayOfWeek() != (periodicityDayInMonth.getDayIndex() == 1 ? 7 : periodicityDayInMonth.getDayIndex() - 1)) {
            slotStart = slotStart.plusDays(1);
        }
        DateTime monthEnd = slotStart.plusMonths(1).minusDays(slotStart.getDayOfMonth() - 1);;
        if (0 < periodicityDayOrder && periodicityDayOrder < 5) {
            while ((slotStart.getDayOfMonth() % 7 == 0 ? slotStart.getDayOfMonth() / 7 : slotStart.getDayOfMonth() / 7 + 1) != periodicityDayOrder) {
                if (slotStart.plusDays(7).isBefore(monthEnd.plusMonths(1))) {
                    slotStart = slotStart.plusDays(7);
                }
            }
        }
        else if (periodicityDayOrder == -1) {
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

    public void updateSlotStartToFutureSlot()
    {
        try {
            setStartDate(getFirstFutureSlotStart());
        }
        catch (IllegalStateException ex) {
            // Continue if model is not set properly yet
            return;
        }
    }

    public LocalDate getFirstFutureSlotStart()
    {
        DateTime slotStart = getRequestStart();
        Period duration = getDuration();

        Period period = getPeriod();
        while (slotStart.plus(duration).isBeforeNow() && period != null) {
            switch (getPeriodicityType()) {
                case WEEKLY:
                    if (periodicDaysInWeek.length > 1) {
                        Set<Integer> daysOfWeek = new HashSet<>();
                        for (PeriodicDateTimeSlot.DayOfWeek day : periodicDaysInWeek) {
                            daysOfWeek.add(day.getDayIndex() == 1 ? 7 : day.getDayIndex() - 1);
                        }
                        while (!daysOfWeek.contains(slotStart.getDayOfWeek()) || slotStart.plus(duration).isBeforeNow()) {
                            slotStart = slotStart.plusDays(1);
                        }
                    } else {
                        slotStart = slotStart.plus(period);
                    }
                    break;
                case MONTHLY:
                    if (PeriodicDateTimeSlot.PeriodicityType.MonthPeriodicityType.SPECIFIC_DAY.equals(getMonthPeriodicityType())) {
                        slotStart = getMonthFirstSlotStart(slotStart.plus(period).withDayOfMonth(1));
                        break;
                    } else {
                        slotStart = slotStart.plus(period);
                    }
                    break;
                case DAILY:
                    slotStart = slotStart.plus(period);
                    break;
                default:
                    throw new TodoImplementException("Unsupported periodicity type: " + getPeriodicityType());
            }
        }
        return slotStart.toLocalDate();
    }

    /**
     * Store all attributes to {@link AbstractReservationRequest}.
     *
     * @return {@link AbstractReservationRequest} with stored attributes
     */
    public AbstractReservationRequest toApi(HttpServletRequest request)
    {
        SortedSet<PeriodicDateTimeSlot> slots = getSlots(UserSession.getInstance(request).getTimeZone());
        // Create reservation request
        AbstractReservationRequest abstractReservationRequest;
        if (periodicityType == PeriodicDateTimeSlot.PeriodicityType.NONE) {
            // Create single reservation request
            ReservationRequest reservationRequest = new ReservationRequest();
            PeriodicDateTimeSlot slot = slots.first();
            reservationRequest.setSlot(slot.getStart(), slot.getStart().plus(slot.getDuration()));
            abstractReservationRequest = reservationRequest;
        }
        else {
            // Create set of reservation requests
            ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
            reservationRequestSet.addAllSlots(slots);
            if (excludeDates != null && !excludeDates.isEmpty()) {
                for (LocalDate excludeDate : excludeDates) {
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
        if (specificationType.equals(SpecificationType.PERMANENT_ROOM)) {
            abstractReservationRequest.setReusement(ReservationRequestReusement.OWNED);
        }
        else if (specificationType.equals(SpecificationType.PERMANENT_ROOM_CAPACITY)) {
            abstractReservationRequest.setReusedReservationRequestId(permanentRoomReservationRequestId);
        }

        // Create specification
        Specification specification = toSpecificationApi();
        abstractReservationRequest.setSpecification(specification);

        // Set reservation request to be deleted by scheduler if foreign resource is specified
        abstractReservationRequest.setIsSchedulerDeleted(!Strings.isNullOrEmpty(getMeetingRoomResourceDomain()));

        return abstractReservationRequest;
    }

    /**
     * @param reservationRequest
     * @return list of {@link Page}s for given {@code reservationRequest}
     */
    public static List<Page> getPagesForBreadcrumb(ReservationRequestSummary reservationRequest)
    {
        SpecificationType specificationType = SpecificationType.fromReservationRequestSummary(reservationRequest);
        return getPagesForBreadcrumb(reservationRequest.getId(), specificationType,
                reservationRequest.getParentReservationRequestId(), reservationRequest.getReusedReservationRequestId());
    }

    /**
     * @param specificationType
     * @param parentReservationRequestId
     * @param permanentRoomReservationRequestId
     * @return list of {@link Page}s for this reservation request
     */
    public static List<Page> getPagesForBreadcrumb(String reservationRequestId,
            SpecificationType specificationType, String parentReservationRequestId,
            String permanentRoomReservationRequestId)
    {
        List<Page> pages = new LinkedList<Page>();

        String titleCode;
        if (parentReservationRequestId != null) {
            if (specificationType.equals(SpecificationType.PERMANENT_ROOM_CAPACITY)) {
                // Add page for permanent room reservation request
                pages.add(new Page(
                        ClientWebUrl.format(ClientWebUrl.DETAIL_VIEW, permanentRoomReservationRequestId),
                        "navigation.detail"));

                // Add page for reservation request set
                pages.add(new Page(
                        ClientWebUrl.format(ClientWebUrl.DETAIL_VIEW, parentReservationRequestId),
                        "navigation.detail.capacity"));
            }
            else {
                // Add page for reservation request set
                pages.add(new Page(
                        ClientWebUrl.format(ClientWebUrl.DETAIL_VIEW, parentReservationRequestId),
                        "navigation.detail"));
            }

            // This reservation request is periodic event
            titleCode = "navigation.detail.event";
        }
        else if (specificationType.equals(SpecificationType.PERMANENT_ROOM_CAPACITY)) {
            // Add page for permanent room reservation request
            pages.add(new Page(
                    ClientWebUrl.format(ClientWebUrl.DETAIL_VIEW, permanentRoomReservationRequestId),
                    "navigation.detail"));

            // This reservation request is capacity
            titleCode = "navigation.detail.capacity";
        }
        else {
            titleCode = "navigation.detail";
        }

        // Add page for this reservation request
        pages.add(new Page(ClientWebUrl.format(ClientWebUrl.DETAIL_VIEW, reservationRequestId), titleCode));

        return pages;
    }

    @Override
    public String toContextString()
    {
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("Type", specificationType);
        attributes.put("Description", description);
        attributes.put("Purpose", purpose);
        attributes.put("Technology", technology);
        attributes.put("Start", start);
        attributes.put("End", end);
        attributes.put("Duration count", durationCount);
        attributes.put("Duration type", durationType);
        attributes.put("Periodicity type", periodicityType);
        attributes.put("Periodicity end", periodicityEnd);
        attributes.put("Room name", roomName);
        attributes.put("Permanent room", permanentRoomReservationRequestId);
        attributes.put("Participant count", roomParticipantCount);
        attributes.put("PIN", roomPin);
        attributes.put("Access mode", roomAccessMode);
        attributes.put("Exclude dates", excludeDates);
        return ReportModel.formatAttributes(attributes);
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
    public boolean createParticipant(ParticipantModel participant, BindingResult bindingResult,
            SecurityToken securityToken)
    {
        participant.validate(bindingResult);
        if (bindingResult.hasErrors()) {
            CommonModel.logValidationErrors(logger, bindingResult, securityToken);
            return false;
        }
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
    public boolean modifyParticipant(String participantId, ParticipantModel participant, BindingResult bindingResult,
            SecurityToken securityToken)
    {
        participant.validate(bindingResult);
        if (bindingResult.hasErrors()) {
            CommonModel.logValidationErrors(logger, bindingResult, securityToken);
            return false;
        }
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
        for (Iterator<ParticipantModel> iterator = roomParticipants.iterator(); iterator.hasNext(); ) {
            ParticipantModel roomParticipant = iterator.next();
            if (roomParticipant.getId() == null) {
                iterator.remove();
            }
        }
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
     * @return default automatically added {@link cz.cesnet.shongo.ParticipantRole} for owner
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

    /**
     * Type of duration unit.
     */
    public static enum DurationType
    {
        MINUTE,
        HOUR,
        DAY
    }

    /**
     * @param reservationService
     * @param securityToken
     * @param cache
     * @return list of reservation requests for permanent rooms
     */
    public static List<ReservationRequestSummary> getPermanentRooms(ReservationService reservationService,
            SecurityToken securityToken, Cache cache)
    {
        ReservationRequestListRequest request = new ReservationRequestListRequest();
        request.setSecurityToken(securityToken);
        request.addSpecificationType(ReservationRequestSummary.SpecificationType.PERMANENT_ROOM);
        List<ReservationRequestSummary> reservationRequests = new LinkedList<ReservationRequestSummary>();

        ListResponse<ReservationRequestSummary> response = reservationService.listReservationRequests(request);
        if (response.getItemCount() > 0) {
            Set<String> reservationRequestIds = new HashSet<String>();
            for (ReservationRequestSummary reservationRequestSummary : response) {
                reservationRequestIds.add(reservationRequestSummary.getId());
            }
            cache.fetchObjectPermissions(securityToken, reservationRequestIds);

            for (ReservationRequestSummary reservationRequestSummary : response) {
                ExecutableState executableState = reservationRequestSummary.getExecutableState();
                if (executableState == null || (!executableState.isAvailable() && !executableState.equals(ExecutableState.NOT_STARTED))) {
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
    public static List<ReservationRequestSummary> getDeleteDependencies(String reservationRequestId,
            ReservationService reservationService, SecurityToken securityToken)
    {
        // List reservation requests which reuse the reservation request to be deleted
        ReservationRequestListRequest reservationRequestListRequest = new ReservationRequestListRequest();
        reservationRequestListRequest.setSecurityToken(securityToken);
        reservationRequestListRequest.setReusedReservationRequestId(reservationRequestId);
        ListResponse<ReservationRequestSummary> reservationRequests =
                reservationService.listReservationRequests(reservationRequestListRequest);
        return reservationRequests.getItems();
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
                ", start=" + start +
                ", end=" + end +
                ", excludeDates=" + excludeDates +
                ", roomName='" + roomName + '\'' +
                ", roomResourceId='" + roomResourceId + '\'' +
                ", meetingRoomResourceId='" + meetingRoomResourceId + '\'' +
                '}';
    }
}
