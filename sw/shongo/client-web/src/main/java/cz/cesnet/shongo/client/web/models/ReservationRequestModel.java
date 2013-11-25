package cz.cesnet.shongo.client.web.models;

import com.google.common.base.Strings;
import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.client.web.support.BreadcrumbItem;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.ClientWebUrl;
import cz.cesnet.shongo.controller.Permission;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestReusement;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.api.ReservationRequestType;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindingResult;

import java.util.*;

/**
 * Model for {@link AbstractReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestModel implements ReportModel.ContextSerializable
{
    private static Logger logger = LoggerFactory.getLogger(ReservationRequestModel.class);

    public static final ReservationRequestPurpose PURPOSE = ReservationRequestPurpose.USER;

    private CacheProvider cacheProvider;

    private String id;

    private String parentReservationRequestId;

    protected ReservationRequestType type;

    protected String description;

    protected DateTime dateTime;

    protected TechnologyModel technology;

    protected DateTime start;

    protected DateTime end;

    protected Integer durationCount;

    protected DurationType durationType;

    protected PeriodicityType periodicityType;

    protected LocalDate periodicityEnd;

    protected SpecificationType specificationType;

    protected String roomName;

    protected String permanentRoomReservationRequestId;

    protected ReservationRequestSummary permanentRoomReservationRequest;

    protected Integer roomParticipantCount;

    protected Integer roomPin;

    protected boolean roomRecorded;

    protected AdobeConnectAccessMode roomAccessMode;

    protected List<UserRoleModel> userRoles = new LinkedList<UserRoleModel>();

    protected List<ParticipantModel> roomParticipants = new LinkedList<ParticipantModel>();

    /**
     * Create new {@link ReservationRequestModel} from scratch.
     */
    public ReservationRequestModel(CacheProvider cacheProvider)
    {
        this.cacheProvider = cacheProvider;
        setStart(Temporal.roundDateTimeToMinutes(DateTime.now(), 1));
        setPeriodicityType(ReservationRequestModel.PeriodicityType.NONE);
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

    public String getRoomName()
    {
        return roomName;
    }

    public void setRoomName(String roomName)
    {
        this.roomName = roomName;
    }

    public String getPermanentRoomReservationRequestId()
    {
        return permanentRoomReservationRequestId;
    }

    public void setPermanentRoomReservationRequestId(String permanentRoomReservationRequestId)
    {
        this.permanentRoomReservationRequestId = permanentRoomReservationRequestId;
    }

    public void setPermanentRoomReservationRequestId(String permanentRoomReservationRequestId,
            List<ReservationRequestSummary> permanentRooms)
    {
        this.permanentRoomReservationRequestId = permanentRoomReservationRequestId;

        if (permanentRoomReservationRequestId != null && start != null) {
            for (ReservationRequestSummary permanentRoomSummary : permanentRooms) {
                if (permanentRoomSummary.getId().equals(permanentRoomReservationRequestId)) {
                    DateTime permanentRoomStart = permanentRoomSummary.getEarliestSlot().getStart();
                    if (permanentRoomStart.isAfter(start)) {
                        setStart(permanentRoomStart);
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

    public Integer getRoomParticipantCount()
    {
        return roomParticipantCount;
    }

    public void setRoomParticipantCount(Integer roomParticipantCount)
    {
        this.roomParticipantCount = roomParticipantCount;
    }

    public Integer getRoomPin()
    {
        return roomPin;
    }

    public void setRoomPin(Integer roomPin)
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

    public AdobeConnectAccessMode getRoomAccessMode()
    {
        return roomAccessMode;
    }

    public void setRoomAccessMode(AdobeConnectAccessMode roomAccessMode)
    {
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
        addRoomParticipant(participantModel);
        return participantModel;
    }

    /**
     * Load attributes from given {@code specification}.
     *
     * @param specification
     * @param reusedReservationRequestId
     */
    public void fromSpecificationApi(Specification specification, String reusedReservationRequestId,
            CacheProvider cacheProvider)
    {
        if (specification instanceof AliasSetSpecification) {
            AliasSetSpecification aliasSetSpecification = (AliasSetSpecification) specification;
            List<AliasSpecification> aliasSpecifications = aliasSetSpecification.getAliases();
            if (aliasSpecifications.size() == 0) {
                throw new UnsupportedApiException("At least one child alias specifications must be present.");
            }
            AliasSpecification roomNameSpecification = aliasSpecifications.get(0);
            fromSpecificationApi(roomNameSpecification, reusedReservationRequestId, cacheProvider);
        }
        else if (specification instanceof AliasSpecification) {
            AliasSpecification aliasSpecification = (AliasSpecification) specification;
            if (!aliasSpecification.isPermanentRoom()) {
                throw new UnsupportedApiException("Alias specification must require permanent room");
            }
            specificationType = SpecificationType.PERMANENT_ROOM;
            technology = TechnologyModel.find(aliasSpecification.getTechnologies());
            roomName = aliasSpecification.getValue();
            Set<AliasType> aliasTypes = aliasSpecification.getAliasTypes();
            if (!(aliasTypes.size() == 1 && aliasTypes.contains(AliasType.ROOM_NAME)
                          && technology != null && roomName != null)) {
                throw new UnsupportedApiException("First alias specification must be for room name.");
            }
            roomParticipants.clear();
            for (AbstractParticipant participant : aliasSpecification.getPermanentRoomParticipants()) {
                roomParticipants.add(new ParticipantModel(participant, cacheProvider));
            }
        }
        else if (specification instanceof RoomSpecification) {
            RoomSpecification roomSpecification = (RoomSpecification) specification;
            if (reusedReservationRequestId != null) {
                permanentRoomReservationRequestId = reusedReservationRequestId;
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
                    try {
                        roomPin = Integer.parseInt(h323RoomSetting.getPin());
                    }
                    catch (NumberFormatException exception) {
                        logger.warn("Failed parsing pin", exception);
                    }
                }
                if (roomSetting instanceof AdobeConnectRoomSetting) {
                    AdobeConnectRoomSetting adobeConnectRoomSetting = (AdobeConnectRoomSetting) roomSetting;
                    try {
                        roomPin = Integer.parseInt(adobeConnectRoomSetting.getPin());
                    }
                    catch (NumberFormatException exception) {
                        logger.warn("Failed parsing pin", exception);
                    }
                    roomAccessMode = adobeConnectRoomSetting.getAccessMode();
                }
            }
            AliasSpecification aliasSpecification = roomSpecification.getAliasSpecificationByType(AliasType.ROOM_NAME);
            if (aliasSpecification != null) {
                roomName = aliasSpecification.getValue();
            }
            roomParticipants.clear();
            for (AbstractParticipant participant : roomSpecification.getParticipants()) {
                roomParticipants.add(new ParticipantModel(participant, cacheProvider));
            }
            for (ExecutableServiceSpecification serviceSpecification : roomSpecification.getServiceSpecifications()) {
                switch (serviceSpecification.getType()) {
                    case RECORDING:
                        roomRecorded = serviceSpecification.isEnabled();
                        break;
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

        // Specification
        Specification specification = abstractReservationRequest.getSpecification();
        fromSpecificationApi(specification, abstractReservationRequest.getReusedReservationRequestId(), cacheProvider);

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
        permanentRoomReservationRequest = cacheProvider.getReservationRequestSummary(permanentRoomReservationRequestId);
        roomName = permanentRoomReservationRequest.getRoomName();
        addPermanentRoomParticipants();
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
                    h323RoomSetting.setPin(roomPin.toString());
                    roomSpecification.addRoomSetting(h323RoomSetting);
                }
                if (technology.equals(TechnologyModel.ADOBE_CONNECT)) {
                    AdobeConnectRoomSetting adobeConnectRoomSetting = new AdobeConnectRoomSetting();
                    if (roomPin != null) {
                        adobeConnectRoomSetting.setPin(roomPin.toString());
                    }
                    adobeConnectRoomSetting.setAccessMode(roomAccessMode);
                    roomSpecification.addRoomSetting(adobeConnectRoomSetting);
                }
                if (roomRecorded && !technology.equals(TechnologyModel.ADOBE_CONNECT)) {
                    roomSpecification.addServiceSpecification(ExecutableServiceSpecification.createRecording());
                }
                for (ParticipantModel participant : roomParticipants) {
                    if (participant.getId() == null) {
                        continue;
                    }
                    roomSpecification.addParticipant(participant.toApi());
                }
                return roomSpecification;
            }
            case PERMANENT_ROOM: {
                AliasSpecification roomNameSpecification = new AliasSpecification();
                roomNameSpecification.addTechnologies(technology.getTechnologies());
                roomNameSpecification.addAliasType(AliasType.ROOM_NAME);
                roomNameSpecification.setValue(roomName);
                roomNameSpecification.setPermanentRoom(true);
                for (ParticipantModel participant : roomParticipants) {
                    if (participant.getId() == null) {
                        continue;
                    }
                    roomNameSpecification.addPermanentRoomParticipant(participant.toApi());
                }
                switch (technology) {
                    case H323_SIP:
                        AliasSpecification numberSpecification = new AliasSpecification();
                        numberSpecification.addAliasType(AliasType.H323_E164);
                        numberSpecification.setPermanentRoom(true);
                        AliasSetSpecification aliasSetSpecification = new AliasSetSpecification();
                        aliasSetSpecification.setSharedExecutable(true);
                        aliasSetSpecification.addAlias(roomNameSpecification);
                        aliasSetSpecification.addAlias(numberSpecification);
                        return aliasSetSpecification;
                    case ADOBE_CONNECT:
                        return roomNameSpecification;
                    default:
                        throw new TodoImplementException(technology);
                }
            }
            case PERMANENT_ROOM_CAPACITY: {
                RoomSpecification roomSpecification = new RoomSpecification();
                roomSpecification.setTechnologies(technology.getTechnologies());
                roomSpecification.setParticipantCount(roomParticipantCount);
                if (technology.equals(TechnologyModel.H323_SIP) && roomPin != null) {
                    H323RoomSetting h323RoomSetting = new H323RoomSetting();
                    h323RoomSetting.setPin(roomPin.toString());
                    roomSpecification.addRoomSetting(h323RoomSetting);
                }
                if (technology.equals(TechnologyModel.ADOBE_CONNECT)) {
                    AdobeConnectRoomSetting adobeConnectRoomSetting = new AdobeConnectRoomSetting();
                    if (roomPin != null) {
                        adobeConnectRoomSetting.setPin(roomPin.toString());
                    }
                    adobeConnectRoomSetting.setAccessMode(roomAccessMode);
                    roomSpecification.addRoomSetting(adobeConnectRoomSetting);
                }
                if (roomRecorded && !technology.equals(TechnologyModel.ADOBE_CONNECT)) {
                    roomSpecification.addServiceSpecification(ExecutableServiceSpecification.createRecording());
                }
                for (ParticipantModel participant : roomParticipants) {
                    if (participant.getId() == null) {
                        continue;
                    }
                    roomSpecification.addParticipant(participant.toApi());
                }
                return roomSpecification;
            }
            default:
                throw new TodoImplementException(specificationType);
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
                return new Period(start.withTime(0, 0, 0, 0), end.withTime(23,59,59,0));
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
                        throw new TodoImplementException(durationType);
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
        switch (specificationType) {
            case PERMANENT_ROOM:
                return new Interval(start.withTime(0, 0, 0, 0), getDuration());
            default:
                return new Interval(start, getDuration());
        }
    }

    /**
     * Store all attributes to {@link AbstractReservationRequest}.
     *
     * @return {@link AbstractReservationRequest} with stored attributes
     */
    public AbstractReservationRequest toApi()
    {
        // Determine slot
        Interval slot = getSlot();

        // Create reservation request
        AbstractReservationRequest abstractReservationRequest;
        if (periodicityType == PeriodicityType.NONE) {
            // Create single reservation request
            ReservationRequest reservationRequest = new ReservationRequest();
            reservationRequest.setSlot(slot.getStart(), slot.getEnd());
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
                    throw new TodoImplementException(durationType);
            }

            // Create set of reservation requests
            ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
            PeriodicDateTimeSlot periodicDateTimeSlot = new PeriodicDateTimeSlot();
            periodicDateTimeSlot.setStart(slot.getStart());
            periodicDateTimeSlot.setDuration(slot.toPeriod());
            periodicDateTimeSlot.setPeriod(period);
            periodicDateTimeSlot.setEnd(periodicityEnd);
            reservationRequestSet.addSlot(periodicDateTimeSlot);
            abstractReservationRequest = reservationRequestSet;
        }
        if (!Strings.isNullOrEmpty(id)) {
            abstractReservationRequest.setId(id);
        }
        abstractReservationRequest.setPurpose(PURPOSE);
        abstractReservationRequest.setDescription(description);
        if (specificationType.equals(SpecificationType.PERMANENT_ROOM_CAPACITY)) {
            abstractReservationRequest.setReusedReservationRequestId(permanentRoomReservationRequestId);
        }
        if (specificationType.equals(SpecificationType.PERMANENT_ROOM)) {
            abstractReservationRequest.setReusement(ReservationRequestReusement.OWNED);
        }

        // Create specification
        abstractReservationRequest.setSpecification(toSpecificationApi());

        return abstractReservationRequest;
    }

    /**
     * @param detailUrl
     * @return list of {@link BreadcrumbItem}s for this reservation request
     */
    public List<BreadcrumbItem> getBreadcrumbItems(String detailUrl)
    {
        List<BreadcrumbItem> breadcrumbItems = new LinkedList<BreadcrumbItem>();

        String titleCode;
        if (parentReservationRequestId != null) {
            if (specificationType.equals(SpecificationType.PERMANENT_ROOM_CAPACITY)) {
                // Add breadcrumb for permanent room reservation request
                breadcrumbItems.add(new BreadcrumbItem(
                        ClientWebUrl.format(detailUrl, permanentRoomReservationRequestId),
                        "navigation.reservationRequest.detail"));

                // Add breadcrumb for reservation request set
                breadcrumbItems.add(new BreadcrumbItem(
                        ClientWebUrl.format(detailUrl, parentReservationRequestId),
                        "navigation.reservationRequest.detailCapacity"));
            }
            else {
                // Add breadcrumb for reservation request set
                breadcrumbItems.add(new BreadcrumbItem(
                        ClientWebUrl.format(detailUrl, parentReservationRequestId),
                        "navigation.reservationRequest.detail"));
            }

            // This reservation request is periodic event
            titleCode = "navigation.reservationRequest.detailEvent";
        }
        else if (specificationType.equals(SpecificationType.PERMANENT_ROOM_CAPACITY)) {
            // Add breadcrumb for permanent room reservation request
            breadcrumbItems.add(new BreadcrumbItem(
                    ClientWebUrl.format(detailUrl, permanentRoomReservationRequestId),
                    "navigation.reservationRequest.detail"));

            // This reservation request is capacity
            titleCode = "navigation.reservationRequest.detailCapacity";
        }
        else {
            titleCode = "navigation.reservationRequest.detail";
        }

        // Add breadcrumb for this reservation request
        breadcrumbItems.add(new BreadcrumbItem(ClientWebUrl.format(detailUrl, id), titleCode));

        return breadcrumbItems;
    }

    @Override
    public String toContextString()
    {
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("ID", id);
        attributes.put("Type", specificationType);
        attributes.put("Description", description);
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
        attributes.put("Access mode",roomAccessMode);
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
            if (possibleParticipant.getId().equals(participantId)) {
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
            if (participant.getType().equals(ParticipantModel.Type.USER) && participant.getUserId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add new participant.
     *
     * @param participant
     * @param participantBindingResult
     */
    public boolean createParticipant(ParticipantModel participant, BindingResult participantBindingResult)
    {
        participant.validate(participantBindingResult);
        if (participantBindingResult.hasErrors()) {
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
     * @param participantBindingResult
     */
    public boolean modifyParticipant(String participantId, ParticipantModel participant,
            BindingResult participantBindingResult)
    {
        participant.validate(participantBindingResult);
        if (participantBindingResult.hasErrors()) {
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
        String permanentRoomReservationId = permanentRoomReservationRequest.getLastReservationId();
        Reservation permanentRoomReservation = cacheProvider.getReservation(permanentRoomReservationId);
        AbstractRoomExecutable permanentRoom = (AbstractRoomExecutable) permanentRoomReservation.getExecutable();
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
                if (executableState == null || (!executableState.isAvailable() && !executableState.equals(ExecutableState.NOT_STARTED))) {
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
        // List reservation requests which reuse the reservation request to be deleted
        ReservationRequestListRequest reservationRequestListRequest = new ReservationRequestListRequest();
        reservationRequestListRequest.setSecurityToken(securityToken);
        reservationRequestListRequest.setReusedReservationRequestId(reservationRequestId);
        ListResponse<ReservationRequestSummary> reservationRequests =
                reservationService.listReservationRequests(reservationRequestListRequest);
        return reservationRequests.getItems();
    }
}
