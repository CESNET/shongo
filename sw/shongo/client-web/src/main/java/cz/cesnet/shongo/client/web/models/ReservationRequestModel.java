package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.AbstractReservationRequest;
import cz.cesnet.shongo.controller.api.ReservationRequest;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

    private DateTime created;

    private String description;

    private ReservationRequestPurpose purpose;

    private Technology technology;

    private DateTime start;

    private DateTime end;

    private Integer durationCount;

    private DurationType durationType;

    private PeriodicityType periodicityType;

    private LocalDate periodicityEnd;

    private Type type;

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

    public DateTime getCreated()
    {
        return created;
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

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
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

    public AbstractReservationRequest toApi()
    {
        throw new TodoImplementException();
    }

    public void fromApi(AbstractReservationRequest reservationRequest)
    {
        id = reservationRequest.getId();
        created = reservationRequest.getCreated();
        description = reservationRequest.getDescription();
        purpose = reservationRequest.getPurpose();
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
        if (type != null) {
            switch (type) {
                case ALIAS:
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "end", "validation.field.required");
                    if (end != null && end.getMillisOfDay() == 0) {
                        end = end.withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59);
                    }
                    if (!start.isBefore(end)) {
                        errors.rejectValue("end", "validation.field.invalidIntervalEnd");
                    }
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "aliasRoomName", "validation.field.required");
                    break;
                case ROOM:
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "durationCount", "validation.field.required");
                    ValidationUtils
                            .rejectIfEmptyOrWhitespace(errors, "roomParticipantCount", "validation.field.required");
                    break;
            }
        }
    }

    public static enum Type
    {
        ALIAS,
        ROOM
    }

    public static enum Technology
    {
        H323_SIP("H.323/SIP", cz.cesnet.shongo.Technology.H323, cz.cesnet.shongo.Technology.SIP),
        ADOBE_CONNECT("Adobe Connect", cz.cesnet.shongo.Technology.ADOBE_CONNECT);

        private final String title;

        private final Set<cz.cesnet.shongo.Technology> technologies;

        private Technology(String title, cz.cesnet.shongo.Technology... technologies)
        {
            this.title = title;
            Set<cz.cesnet.shongo.Technology> technologySet = new HashSet<cz.cesnet.shongo.Technology>();
            for (cz.cesnet.shongo.Technology technology : technologies) {
                technologySet.add(technology);
            }
            this.technologies = Collections.unmodifiableSet(technologySet);
        }

        public String getTitle()
        {
            return title;
        }

        public Set<cz.cesnet.shongo.Technology> getTechnologies()
        {
            return technologies;
        }

        public static Technology find(Set<cz.cesnet.shongo.Technology> technologies)
        {
            if (H323_SIP.technologies.containsAll(technologies)) {
                return H323_SIP;
            }
            else if (ADOBE_CONNECT.technologies.containsAll(technologies)) {
                return ADOBE_CONNECT;
            }
            return null;
        }
    }

    public static enum DurationType
    {
        MINUTE,
        HOUR,
        DAY
    }

    public static enum PeriodicityType
    {
        NONE,
        DAILY,
        WEEKLY
    }
}
