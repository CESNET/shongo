package cz.cesnet.shongo.client.web.models;

import com.google.common.base.Strings;
import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.H323RoomSetting;
import cz.cesnet.shongo.api.RoomSetting;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.ReservationRequestType;
import cz.cesnet.shongo.controller.api.*;
import org.joda.time.*;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.util.*;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestModel implements Validator
{
    public ReservationRequestModel()
    {
    }

    public ReservationRequestModel(AbstractReservationRequest reservationRequest)
    {
        fromApi(reservationRequest);
    }

    private String id;

    private ReservationRequestType type;

    private DateTime dateTime;

    private String description;

    private ReservationRequestPurpose purpose;

    private Technology technology;

    private DateTime start;

    private DateTime end;

    private Integer durationCount;

    private DurationType durationType;

    private PeriodicityType periodicityType;

    private LocalDate periodicityEnd;

    private SpecificationType specificationType;

    private String aliasRoomName;

    private String roomAliasReservationId;

    private Integer roomParticipantCount;

    private String roomPin;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
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

    public Technology getTechnology()
    {
        return technology;
    }

    public void setTechnology(Technology technology)
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

    public String getAliasRoomName()
    {
        return aliasRoomName;
    }

    public void setAliasRoomName(String aliasRoomName)
    {
        this.aliasRoomName = aliasRoomName;
    }

    public String getRoomAliasReservationId()
    {
        return roomAliasReservationId;
    }

    public void setRoomAliasReservationId(String roomAliasReservationId)
    {
        this.roomAliasReservationId = roomAliasReservationId;
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

    /**
     * Load attributes from given {@code abstractReservationRequest}.
     *
     * @param abstractReservationRequest from which the attributes should be loaded
     */
    public void fromApi(AbstractReservationRequest abstractReservationRequest)
    {
        id = abstractReservationRequest.getId();
        type = abstractReservationRequest.getType();
        dateTime = abstractReservationRequest.getDateTime();
        description = abstractReservationRequest.getDescription();
        purpose = abstractReservationRequest.getPurpose();

        // Specification
        Specification specification = abstractReservationRequest.getSpecification();
        if (specification instanceof AliasSpecification) {
            AliasSpecification aliasSpecification = (AliasSpecification) specification;
            specificationType = SpecificationType.ALIAS;
            technology = Technology.find(aliasSpecification.getTechnologies());
            aliasRoomName = aliasSpecification.getValue();
            Set<AliasType> aliasTypes = aliasSpecification.getAliasTypes();
            if (!(aliasTypes.size() == 1 && aliasTypes.contains(AliasType.ROOM_NAME)
                          && technology != null && aliasRoomName != null)) {
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
            specificationType = SpecificationType.ALIAS;
            technology = Technology.find(roomNameSpecification.getTechnologies());
            aliasRoomName = roomNameSpecification.getValue();
            Set<AliasType> aliasTypes = roomNameSpecification.getAliasTypes();
            if (!(aliasTypes.size() == 1 && aliasTypes.contains(AliasType.ROOM_NAME)
                          && technology != null && aliasRoomName != null)) {
                throw new UnsupportedApiException("First alias specification must be for room name.");
            }
        }
        else if (specification instanceof RoomSpecification) {
            RoomSpecification roomSpecification = (RoomSpecification) specification;
            specificationType = SpecificationType.ROOM;
            technology = Technology.find(roomSpecification.getTechnologies());
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

        // Date/time slot and periodicity
        Period duration;
        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;
            periodicityType = PeriodicityType.NONE;
            Interval slot = reservationRequest.getSlot();
            start = slot.getStart();
            end = slot.getEnd();
            duration = slot.toPeriod();
        }
        else if (abstractReservationRequest instanceof ReservationRequestSet) {
            ReservationRequestSet reservationRequestSet = (ReservationRequestSet) abstractReservationRequest;
            List<Object> slots = reservationRequestSet.getSlots();
            if (!(slots.size() == 1 && slots.get(0) instanceof PeriodicDateTimeSlot)) {
                throw new UnsupportedApiException("Only single periodic date/time slot is allowed.");
            }
            if (!specificationType.equals(SpecificationType.ROOM)) {
                throw new UnsupportedApiException("Periodicity is allowed only for rooms.");
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
        if (specificationType.equals(SpecificationType.ROOM)) {
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
     * Store all attributes to {@link AbstractReservationRequest}.
     *
     * @return {@link AbstractReservationRequest} with stored attributes
     */
    public AbstractReservationRequest toApi()
    {
        // Determine duration (and end)
        Period duration = null;
        if (specificationType.equals(SpecificationType.ALIAS)) {
            if (end == null) {
                throw new IllegalStateException("Slot end must be not empty for alias.");
            }
            duration = new Period(start, end);
        }
        if (specificationType.equals(SpecificationType.ROOM)) {
            if (durationCount == null || durationType == null) {
                throw new IllegalStateException("Slot duration should be not empty.");
            }
            switch (durationType) {
                case MINUTE:
                    duration = Period.minutes(durationCount);
                    break;
                case HOUR:
                    duration = Period.hours(durationCount);
                    break;
                case DAY:
                    duration = Period.days(durationCount);
                    break;
                default:
                    throw new TodoImplementException(durationType.toString());
            }
            end = start.plus(duration);
        }

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

        // Create specification
        Specification specification;
        switch (specificationType) {
            case ALIAS:
                AliasSpecification roomNameSpecification = new AliasSpecification();
                roomNameSpecification.addTechnologies(technology.getTechnologies());
                roomNameSpecification.addAliasType(AliasType.ROOM_NAME);
                roomNameSpecification.setValue(aliasRoomName);
                switch (technology) {
                    case H323_SIP:
                        AliasSpecification numberSpecification = new AliasSpecification();
                        numberSpecification.addAliasType(AliasType.H323_E164);
                        AliasSetSpecification aliasSetSpecification = new AliasSetSpecification();
                        aliasSetSpecification.setSharedExecutable(true);
                        aliasSetSpecification.addAlias(roomNameSpecification);
                        aliasSetSpecification.addAlias(numberSpecification);
                        specification = aliasSetSpecification;
                        break;
                    case ADOBE_CONNECT:
                        specification = roomNameSpecification;
                        break;
                    default:
                        throw new TodoImplementException(technology.toString());
                }
                break;
            case ROOM:
                RoomSpecification roomSpecification = new RoomSpecification();
                roomSpecification.setTechnologies(technology.getTechnologies());
                roomSpecification.setParticipantCount(roomParticipantCount);
                if (technology.equals(Technology.H323_SIP) && roomPin != null) {
                    H323RoomSetting h323RoomSetting = new H323RoomSetting();
                    h323RoomSetting.setPin(roomPin);
                    roomSpecification.addRoomSetting(h323RoomSetting);
                }
                if (!Strings.isNullOrEmpty(roomAliasReservationId)) {
                    throw new TodoImplementException("Provide alias reservation");
                }
                specification = roomSpecification;
                break;
            default:
                throw new TodoImplementException(specificationType.toString());
        }
        abstractReservationRequest.setSpecification(specification);

        return abstractReservationRequest;
    }

    /**
     * Validate this {@link ReservationRequestModel}.
     *
     * @param errors
     */
    public void validate(Errors errors)
    {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "purpose", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "description", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "technology", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "start", "validation.field.required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "specificationType", "validation.field.required");
        if (specificationType != null) {
            switch (specificationType) {
                case ALIAS:
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "end", "validation.field.required");
                    if (end != null && end.getMillisOfDay() == 0) {
                        end = end.withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59);
                    }
                    if (start != null && end != null && !start.isBefore(end)) {
                        errors.rejectValue("end", "validation.field.invalidIntervalEnd");
                    }
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "aliasRoomName", "validation.field.required");
                    break;
                case ROOM:
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors,"durationCount", "validation.field.required");
                    ValidationUtils.rejectIfEmptyOrWhitespace(
                            errors, "roomParticipantCount", "validation.field.required");
                    break;
            }
        }
    }

    @Override
    public boolean supports(Class<?> type)
    {
        return ReservationRequestModel.class.equals(type);
    }

    @Override
    public void validate(Object object, Errors errors)
    {
        ReservationRequestModel reservationRequestModel = (ReservationRequestModel) object;
        reservationRequestModel.validate(errors);
    }

    /**
     * Type of the reservation request.
     */
    public static enum SpecificationType
    {
        /**
         * For alias (or set of aliases).
         */
        ALIAS,

        /**
         * For room capacity.
         */
        ROOM
    }

    /**
     * Technology of the alias/room reservation request.
     */
    public static enum Technology
    {
        /**
         * {@link cz.cesnet.shongo.Technology#H323} and/or {@link cz.cesnet.shongo.Technology#SIP}
         */
        H323_SIP("H.323/SIP", cz.cesnet.shongo.Technology.H323, cz.cesnet.shongo.Technology.SIP),

        /**
         * {@link cz.cesnet.shongo.Technology#ADOBE_CONNECT}
         */
        ADOBE_CONNECT("Adobe Connect", cz.cesnet.shongo.Technology.ADOBE_CONNECT);

        /**
         * Title which can be displayed to user.
         */
        private final String title;

        /**
         * Set of {@link cz.cesnet.shongo.Technology}s which it represents.
         */
        private final Set<cz.cesnet.shongo.Technology> technologies;

        /**
         * Constructor.
         *
         * @param title sets the {@link #title}
         * @param technologies sets the {@link #technologies}
         */
        private Technology(String title, cz.cesnet.shongo.Technology... technologies)
        {
            this.title = title;
            Set<cz.cesnet.shongo.Technology> technologySet = new HashSet<cz.cesnet.shongo.Technology>();
            for (cz.cesnet.shongo.Technology technology : technologies) {
                technologySet.add(technology);
            }
            this.technologies = Collections.unmodifiableSet(technologySet);
        }

        /**
         * @return {@link #title}
         */
        public String getTitle()
        {
            return title;
        }

        /**
         * @return {@link #technologies}
         */
        public Set<cz.cesnet.shongo.Technology> getTechnologies()
        {
            return technologies;
        }

        /**
         * @param technologies which must the returned {@link Technology} contain
         * @return {@link Technology} which contains all given {@code technologies}
         */
        public static Technology find(Set<cz.cesnet.shongo.Technology> technologies)
        {
            if (technologies.size() == 0) {
                return null;
            }
            if (H323_SIP.technologies.containsAll(technologies)) {
                return H323_SIP;
            }
            else if (ADOBE_CONNECT.technologies.containsAll(technologies)) {
                return ADOBE_CONNECT;
            }
            return null;
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
}
