package cz.cesnet.shongo.controller.api.domains.request;

import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.AdobeConnectPermissions;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.Reservation;
import org.joda.time.Interval;

import java.util.*;

/**
 * Represents a room settings for allocating foreign {@link Reservation}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class RoomSettings
{
    private Interval slot;
    private int participantCount;
    private String userId;
    private Map<UserInformation, ParticipantRole> participants;
    private List<Set<Technology>> technologyVariants;
    private String description;
    private String roomName;
    private String roomPin;
    private AdobeConnectPermissions acAccessMode;
    private boolean recordRoom;

    public Interval getSlot()
    {
        return slot;
    }

    public void setSlot(Interval slot)
    {
        this.slot = slot;
    }

    public int getParticipantCount()
    {
        return participantCount;
    }

    public void setParticipantCount(int participantCount)
    {
        this.participantCount = participantCount;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public Map<UserInformation, ParticipantRole> getParticipants()
    {
        return participants;
    }

    public void setParticipants(Map<UserInformation, ParticipantRole> participants)
    {
        this.participants = participants;
    }

    public void addParticipant(UserInformation participant, ParticipantRole role)
    {
        if (this.participants == null) {
            this.participants = new HashMap<>();
        }
        this.participants.put(participant, role);
    }

    public List<Set<Technology>> getTechnologyVariants()
    {
        return technologyVariants;
    }

    public Set<Technology> getFirstTechnologyVariant()
    {
        return technologyVariants.get(0);
    }

    public void setTechnologyVariants(List<Set<Technology>> technologyVariants)
    {
        this.technologyVariants = technologyVariants;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getRoomName()
    {
        return roomName;
    }

    public void setRoomName(String roomName)
    {
        this.roomName = roomName;
    }

    public String getRoomPin()
    {
        return roomPin;
    }

    public void setRoomPin(String roomPin)
    {
        this.roomPin = roomPin;
    }

    public AdobeConnectPermissions getAcAccessMode()
    {
        return acAccessMode;
    }

    public void setAcAccessMode(AdobeConnectPermissions acAccessMode)
    {
        this.acAccessMode = acAccessMode;
    }

    public boolean isRecordRoom()
    {
        return recordRoom;
    }

    public void setRecordRoom(Boolean recordRoom)
    {
        this.recordRoom = recordRoom == null ? false : recordRoom;
    }

    public void setRecordRoom(boolean recordRoom)
    {
        this.recordRoom = recordRoom;
    }

    public void validate()
    {
        if (slot == null) {
            throw new IllegalArgumentException("Slot must be set.");
        }
        if (participantCount < 1) {
            throw new IllegalArgumentException("Participant count must be set (grater than 0).");
        }
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("UserId must be set.");
        }
        if (technologyVariants == null  || technologyVariants.isEmpty()) {
            throw new IllegalArgumentException("Technology must be set.");
        }
        if (technologyVariants.size() > 1) {
            throw new TodoImplementException();
        } else {
            if (!getFirstTechnologyVariant().contains(Technology.ADOBE_CONNECT) && acAccessMode != null) {
                throw new IllegalStateException("Cannot use Adobe Connect access mode when technology is missing");
            }
        }
        if (roomName != null) {
            throw new TodoImplementException();
        }
    }
}
