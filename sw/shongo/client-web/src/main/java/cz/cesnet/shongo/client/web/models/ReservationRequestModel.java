package cz.cesnet.shongo.client.web.models;

import com.google.common.base.Strings;
import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.H323RoomSetting;
import cz.cesnet.shongo.api.RoomSetting;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.MessageProvider;
import cz.cesnet.shongo.controller.Permission;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.api.ReservationRequestType;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.*;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestModel
{
    private String id;

    private String parentReservationRequestId;

    private ReservationRequestType type;

    private DateTime dateTime;

    private String description;

    private ReservationRequestPurpose purpose;

    private TechnologyModel technology;

    private DateTime start;

    private DateTime end;

    private Integer durationCount;

    private DurationType durationType;

    private PeriodicityType periodicityType;

    private LocalDate periodicityEnd;

    private SpecificationType specificationType;

    private String permanentRoomName;

    private String permanentRoomReservationRequestId;

    private ReservationRequestSummary permanentRoomReservationRequest;

    private Integer roomParticipantCount;

    private String roomPin;

    private List<UserRoleModel> userRoles = new LinkedList<UserRoleModel>();

    public ReservationRequestModel()
    {
        setStart(Temporal.roundDateTimeToMinutes(DateTime.now(), 1));
        setPeriodicityType(ReservationRequestModel.PeriodicityType.NONE);
    }

    public ReservationRequestModel(AbstractReservationRequest reservationRequest, CacheProvider cacheProvider)
    {
        fromApi(reservationRequest, cacheProvider);
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

    public void setDescription(String description)
    {
        this.description = description;
    }

    public ReservationRequestPurpose getPurpose()
    {
        return purpose;
    }

    public void setPurpose(ReservationRequestPurpose purpose)
    {
        this.purpose = purpose;
    }

    public TechnologyModel getTechnology()
    {
        return technology;
    }

    public void setTechnology(TechnologyModel technology)
    {
        this.technology = technology;
    }

    public DateTime getStart()
    {
        return start;
    }

    public void setStart(DateTime start)
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

    public PeriodicityType getPeriodicityType()
    {
        return periodicityType;
    }

    public void setPeriodicityType(PeriodicityType periodicityType)
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

    public SpecificationType getSpecificationType()
    {
        return specificationType;
    }

    public void setSpecificationType(SpecificationType specificationType)
    {
        this.specificationType = specificationType;
    }

    public String getPermanentRoomName()
    {
        return permanentRoomName;
    }

    public void setPermanentRoomName(String permanentRoomName)
    {
        this.permanentRoomName = permanentRoomName;
    }

    public String getPermanentRoomReservationRequestId()
    {
        return permanentRoomReservationRequestId;
    }

    public void setPermanentRoomReservationRequestId(String permanentRoomReservationRequestId)
    {
        this.permanentRoomReservationRequestId = permanentRoomReservationRequestId;
    }

    public ReservationRequestSummary getPermanentRoomReservationRequest()
    {
        return permanentRoomReservationRequest;
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

    public UserRoleModel addUserRole(UserInformation userInformation, Role role)
    {
        UserRoleModel userRole = new UserRoleModel(userInformation);
        userRole.setEntityId(id);
        userRole.setRole(role);
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

    /**
     * Load attributes from given {@code specification}.
     *
     * @param specification
     * @param providedReservationRequestId
     */
    public void fromSpecificationApi(Specification specification, String providedReservationRequestId)
    {
        if (specification instanceof AliasSpecification) {
            AliasSpecification aliasSpecification = (AliasSpecification) specification;
            specificationType = SpecificationType.PERMANENT_ROOM;
            technology = TechnologyModel.find(aliasSpecification.getTechnologies());
            permanentRoomName = aliasSpecification.getValue();
            Set<AliasType> aliasTypes = aliasSpecification.getAliasTypes();
            if (!(aliasTypes.size() == 1 && aliasTypes.contains(AliasType.ROOM_NAME)
                          && technology != null && permanentRoomName != null)) {
                throw new UnsupportedApiException("Alias specification must be for room name.");
            }
        }
        else if (specification instanceof AliasSetSpecification) {
            AliasSetSpecification aliasSetSpecification = (AliasSetSpecification) specification;
            List<AliasSpecification> aliasSpecifications = aliasSetSpecification.getAliases();
            if (aliasSpecifications.size() == 0) {
                throw new UnsupportedApiException("At least one child alias specifications must be present.");
            }
            AliasSpecification roomNameSpecification = aliasSpecifications.get(0);
            specificationType = SpecificationType.PERMANENT_ROOM;
            technology = TechnologyModel.find(roomNameSpecification.getTechnologies());
            permanentRoomName = roomNameSpecification.getValue();
            Set<AliasType> aliasTypes = roomNameSpecification.getAliasTypes();
            if (!(aliasTypes.size() == 1 && aliasTypes.contains(AliasType.ROOM_NAME)
                          && technology != null && permanentRoomName != null)) {
                throw new UnsupportedApiException("First alias specification must be for room name.");
            }
        }
        else if (specification instanceof RoomSpecification) {
            RoomSpecification roomSpecification = (RoomSpecification) specification;
            if (providedReservationRequestId != null) {
                permanentRoomReservationRequestId = providedReservationRequestId;
                specificationType = SpecificationType.PERMANENT_ROOM_CAPACITY;
            }
            else {
                specificationType = SpecificationType.ADHOC_ROOM;
            }
            technology = TechnologyModel.find(roomSpecification.getTechnologies());
            roomParticipantCount = roomSpecification.getParticipantCount();
            for (RoomSetting roomSetting : roomSpecification.getRoomSettings()) {
                if (roomSetting instanceof H323RoomSetting) {
                    H323RoomSetting h323RoomSetting = (H323RoomSetting) roomSetting;
                    roomPin = h323RoomSetting.getPin();
                }
            }
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
        purpose = abstractReservationRequest.getPurpose();

        // Specification
        Specification specification = abstractReservationRequest.getSpecification();
        fromSpecificationApi(specification, abstractReservationRequest.getProvidedReservationRequestId());

        // Date/time slot and periodicity
        Period duration;
        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;
            periodicityType = PeriodicityType.NONE;
            Interval slot = reservationRequest.getSlot();
            start = slot.getStart();
            end = slot.getEnd();
            duration = slot.toPeriod();

            parentReservationRequestId = reservationRequest.getParentReservationRequestId();
        }
        else if (abstractReservationRequest instanceof ReservationRequestSet) {
            ReservationRequestSet reservationRequestSet = (ReservationRequestSet) abstractReservationRequest;
            List<Object> slots = reservationRequestSet.getSlots();
            if (!(slots.size() == 1 && slots.get(0) instanceof PeriodicDateTimeSlot)) {
                throw new UnsupportedApiException("Only single periodic date/time slot is allowed.");
            }
            if (specificationType.equals(SpecificationType.PERMANENT_ROOM)) {
                throw new UnsupportedApiException("Periodicity is not allowed for permanent rooms.");
            }
            PeriodicDateTimeSlot slot = (PeriodicDateTimeSlot) slots.get(0);
            start = slot.getStart();
            duration = slot.getDuration();
            end = start.plus(duration);

            Period period = slot.getPeriod();
            if (period.equals(Period.days(1))) {
                periodicityType = PeriodicityType.DAILY;
            }
            else if (period.equals(Period.weeks(1))) {
                periodicityType = PeriodicityType.WEEKLY;
            }
            else {
                throw new UnsupportedApiException("Periodicity %s.", period);
            }

            ReadablePartial slotEnd = slot.getEnd();
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
        }
        else {
            throw new UnsupportedApiException(abstractReservationRequest);
        }

        // Room duration
        if (!specificationType.equals(SpecificationType.PERMANENT_ROOM)) {
            int minutes = duration.toStandardMinutes().getMinutes();
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
        }

        // Additional specification attributes
        switch (specificationType) {
            case PERMANENT_ROOM_CAPACITY:
                if (cacheProvider != null) {
                    loadPermanentRoom(cacheProvider);
                }
                break;
        }
    }

    /**
     * Load {@link #permanentRoomReservationRequest} by given {@code cacheProvider}.
     *
     * @param cacheProvider
     */
    public void loadPermanentRoom(CacheProvider cacheProvider)
    {
        if (permanentRoomReservationRequestId == null) {
            throw new UnsupportedApiException("Permanent room capacity should have permanent room set.");
        }
        permanentRoomReservationRequest =
                cacheProvider.getReservationRequestSummary(permanentRoomReservationRequestId);
    }

    /**
     * Store attributes to {@link Specification}.
     *
     * @return {@link Specification} with stored attributes
     */
    public Specification toSpecificationApi()
    {
        switch (specificationType) {
            case ADHOC_ROOM: {
                RoomSpecification roomSpecification = new RoomSpecification();
                roomSpecification.setTechnologies(technology.getTechnologies());
                roomSpecification.setParticipantCount(roomParticipantCount);
                if (technology.equals(TechnologyModel.H323_SIP) && roomPin != null) {
                    H323RoomSetting h323RoomSetting = new H323RoomSetting();
                    h323RoomSetting.setPin(roomPin);
                    roomSpecification.addRoomSetting(h323RoomSetting);
                }
                return roomSpecification;
            }
            case PERMANENT_ROOM: {
                AliasSpecification roomNameSpecification = new AliasSpecification();
                roomNameSpecification.addTechnologies(technology.getTechnologies());
                roomNameSpecification.addAliasType(AliasType.ROOM_NAME);
                roomNameSpecification.setValue(permanentRoomName);
                switch (technology) {
                    case H323_SIP:
                        AliasSpecification numberSpecification = new AliasSpecification();
                        numberSpecification.addAliasType(AliasType.H323_E164);
                        AliasSetSpecification aliasSetSpecification = new AliasSetSpecification();
                        aliasSetSpecification.setSharedExecutable(true);
                        aliasSetSpecification.addAlias(roomNameSpecification);
                        aliasSetSpecification.addAlias(numberSpecification);
                        return aliasSetSpecification;
                    case ADOBE_CONNECT:
                        return roomNameSpecification;
                    default:
                        throw new TodoImplementException(technology.toString());
                }
            }
            case PERMANENT_ROOM_CAPACITY: {
                RoomSpecification roomSpecification = new RoomSpecification();
                roomSpecification.setTechnologies(technology.getTechnologies());
                roomSpecification.setParticipantCount(roomParticipantCount);
                if (technology.equals(TechnologyModel.H323_SIP) && roomPin != null) {
                    H323RoomSetting h323RoomSetting = new H323RoomSetting();
                    h323RoomSetting.setPin(roomPin);
                    roomSpecification.addRoomSetting(h323RoomSetting);
                }
                return roomSpecification;
            }
            default:
                throw new TodoImplementException(specificationType.toString());
        }
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
                if (!start.equals(end)) {
                    return new Period(start, end);
                }
                else {
                    return new Period(start.withTime(0, 0, 0, 0), end.withTime(23,59,59,0));
                }
            case ADHOC_ROOM:
            case PERMANENT_ROOM_CAPACITY:
                if (durationCount == null || durationType == null) {
                    throw new IllegalStateException("Slot duration should be not empty.");
                }
                switch (durationType) {
                    case MINUTE:
                        return Period.minutes(durationCount);
                    case HOUR:
                        return Period.hours(durationCount);
                    case DAY:
                        return Period.days(durationCount);
                    default:
                        throw new TodoImplementException(durationType.toString());
                }
            default:
                throw new TodoImplementException("Reservation request duration.");
        }
    }

    /**
     * @return requested reservation date/time slot as {@link Interval}
     */
    public Interval getSlot()
    {
        return new Interval(start, getDuration());
    }

    /**
     * Store all attributes to {@link AbstractReservationRequest}.
     *
     * @return {@link AbstractReservationRequest} with stored attributes
     */
    public AbstractReservationRequest toApi()
    {
        // Determine duration (and end)
        Period duration = getDuration();
        end = start.plus(duration);

        // Create reservation request
        AbstractReservationRequest abstractReservationRequest;
        if (periodicityType == PeriodicityType.NONE) {
            // Create single reservation request
            ReservationRequest reservationRequest = new ReservationRequest();
            reservationRequest.setSlot(start, end);
            abstractReservationRequest = reservationRequest;
        }
        else {
            // Determine period
            Period period;
            switch (periodicityType) {
                case DAILY:
                    period = Period.days(1);
                    break;
                case WEEKLY:
                    period = Period.weeks(1);
                    break;
                default:
                    throw new TodoImplementException(durationType.toString());
            }

            // Create set of reservation requests
            ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
            PeriodicDateTimeSlot periodicDateTimeSlot = new PeriodicDateTimeSlot();
            periodicDateTimeSlot.setStart(start);
            periodicDateTimeSlot.setDuration(duration);
            periodicDateTimeSlot.setPeriod(period);
            periodicDateTimeSlot.setEnd(periodicityEnd);
            reservationRequestSet.addSlot(periodicDateTimeSlot);
            abstractReservationRequest = reservationRequestSet;
        }
        if (!Strings.isNullOrEmpty(id)) {
            abstractReservationRequest.setId(id);
        }
        abstractReservationRequest.setPurpose(purpose);
        abstractReservationRequest.setDescription(description);
        if (specificationType.equals(SpecificationType.PERMANENT_ROOM_CAPACITY)) {
            abstractReservationRequest.setProvidedReservationRequestId(permanentRoomReservationRequestId);
        }

        // Create specification
        abstractReservationRequest.setSpecification(toSpecificationApi());

        return abstractReservationRequest;
    }

    /**
     * Type of the reservation request.
     */
    public static enum SpecificationType
    {
        /**
         * For ad-hoc room.
         */
        ADHOC_ROOM(true),

        /**
         * For permanent room.
         */
        PERMANENT_ROOM(true),

        /**
         * For permanent room capacity.
         */
        PERMANENT_ROOM_CAPACITY(false);

        private final boolean isRoom;

        private SpecificationType(boolean isRoom)
        {
            this.isRoom = isRoom;
        }

        public boolean isRoom()
        {
            return isRoom;
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
     * Type of periodicity of the reservation request.
     */
    public static enum PeriodicityType
    {
        NONE,
        DAILY,
        WEEKLY
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
        request.addSpecificationClass(AliasSpecification.class);
        request.addSpecificationClass(AliasSetSpecification.class);
        List<ReservationRequestSummary> reservationRequests = new LinkedList<ReservationRequestSummary>();

        ListResponse<ReservationRequestSummary> response = reservationService.listReservationRequests(request);
        if (response.getItemCount() > 0) {
            Set<String> reservationRequestIds = new HashSet<String>();
            for (ReservationRequestSummary reservationRequestSummary : response) {
                reservationRequestIds.add(reservationRequestSummary.getId());
            }
            cache.fetchPermissions(securityToken, reservationRequestIds);

            for (ReservationRequestSummary reservationRequestSummary : response) {
                ExecutableState executableState = reservationRequestSummary.getExecutableState();
                if (executableState == null || !executableState.isAvailable()) {
                    continue;
                }
                Set<Permission> permissions = cache.getPermissions(securityToken, reservationRequestSummary.getId());
                if (!permissions.contains(Permission.PROVIDE_RESERVATION_REQUEST)) {
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
        // List reservation requests which got provided the reservation request to be deleted
        ReservationRequestListRequest reservationRequestListRequest = new ReservationRequestListRequest();
        reservationRequestListRequest.setSecurityToken(securityToken);
        reservationRequestListRequest.setProvidedReservationRequestId(reservationRequestId);
        ListResponse<ReservationRequestSummary> reservationRequests =
                reservationService.listReservationRequests(reservationRequestListRequest);
        return reservationRequests.getItems();
    }
}
