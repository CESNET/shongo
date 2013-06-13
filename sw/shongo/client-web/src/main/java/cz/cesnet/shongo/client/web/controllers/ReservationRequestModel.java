package cz.cesnet.shongo.client.web.controllers;

import cz.cesnet.shongo.Technology;
import org.joda.time.DateTime;
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
    private String id;

    private DateTime start;

    private DateTime end;

    private Integer durationCount;

    private DurationType durationType;

    private String description;

    private String purpose;

    private Technology technology;

    private SpecificationType type;

    private AliasSpecification alias;

    private RoomSpecification room;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
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

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getPurpose()
    {
        return purpose;
    }

    public void setPurpose(String purpose)
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

    public SpecificationType getType()
    {
        return type;
    }

    public void setType(SpecificationType type)
    {
        this.type = type;
    }

    public AliasSpecification getAlias()
    {
        return alias;
    }

    public void setAlias(AliasSpecification alias)
    {
        this.alias = alias;
    }

    public RoomSpecification getRoom()
    {
        return room;
    }

    public void setRoom(RoomSpecification room)
    {
        this.room = room;
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
                    if (end.getMillisOfDay() == 0) {
                        end = end.withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59);
                    }
                    if (!start.isBefore(end)) {
                        errors.rejectValue("end", "validation.field.invalidIntervalEnd");
                    }
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "alias.roomName", "validation.field.required");
                    break;
                case ROOM:
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "durationCount", "validation.field.required");
                    ValidationUtils
                            .rejectIfEmptyOrWhitespace(errors, "room.participantCount", "validation.field.required");
                    break;
            }
        }
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

    public static enum SpecificationType
    {
        ALIAS,
        ROOM
    }

    public static class AliasSpecification
    {
        private String roomName;

        public String getRoomName()
        {
            return roomName;
        }

        public void setRoomName(String roomName)
        {
            this.roomName = roomName;
        }
    }

    public static class RoomSpecification
    {
        private String alias;

        private Integer participantCount;

        private String pin;

        public String getAlias()
        {
            return alias;
        }

        public void setAlias(String alias)
        {
            this.alias = alias;
        }

        public Integer getParticipantCount()
        {
            return participantCount;
        }

        public void setParticipantCount(Integer participantCount)
        {
            this.participantCount = participantCount;
        }

        public String getPin()
        {
            return pin;
        }

        public void setPin(String pin)
        {
            this.pin = pin;
        }
    }
}
