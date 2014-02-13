package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.DataMap;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents that room should be made available to participants for joining (e.g. a meeting will take place there).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomAvailability extends AbstractComplexType
{
    /**
     * Specifies the name of the meeting which will take place in the room.
     */
    private String meetingName;

    /**
     * Specifies the description of the meeting which will take place in the room.
     */
    private String meetingDescription;

    /**
     * Number of minutes which the room shall be available before requested time slot.
     */
    private int slotMinutesBefore = 0;

    /**
     * Number of minutes which the room shall be available after requested time slot.
     */
    private int slotMinutesAfter = 0;

    /**
     * Number of ports which must be allocated for the virtual room.
     */
    private int participantCount;

    /**
     * Specifies whether configured participants should be notified about the room events.
     */
    private boolean participantNotificationEnabled = false;

    /**
     * {@link cz.cesnet.shongo.controller.api.ExecutableServiceSpecification}s for the virtual room.
     */
    private List<ExecutableServiceSpecification> serviceSpecifications = new LinkedList<ExecutableServiceSpecification>();

    /**
     * Constructor.
     */
    public RoomAvailability()
    {
    }

    /**
     * Constructor.
     *
     * @param participantCount sets the {@link #participantCount}
     */
    public RoomAvailability(int participantCount)
    {
        this.participantCount = participantCount;
    }

    /**
     * @return {@link #meetingName}
     */
    public String getMeetingName()
    {
        return meetingName;
    }

    /**
     * @param meetingName sets the {@link #meetingName}
     */
    public void setMeetingName(String meetingName)
    {
        this.meetingName = meetingName;
    }

    /**
     * @return {@link #meetingDescription}
     */
    public String getMeetingDescription()
    {
        return meetingDescription;
    }

    /**
     * @param meetingDescription sets the {@link #meetingDescription}
     */
    public void setMeetingDescription(String meetingDescription)
    {
        this.meetingDescription = meetingDescription;
    }

    /**
     * @return {@link #slotMinutesBefore}
     */
    public int getSlotMinutesBefore()
    {
        return slotMinutesBefore;
    }

    /**
     * @param slotMinutesBefore sets the {@link #slotMinutesBefore}
     */
    public void setSlotMinutesBefore(int slotMinutesBefore)
    {
        this.slotMinutesBefore = slotMinutesBefore;
    }

    /**
     * @return {@link #slotMinutesAfter}
     */
    public int getSlotMinutesAfter()
    {
        return slotMinutesAfter;
    }

    /**
     * @param slotMinutesAfter {@link #slotMinutesAfter}
     */
    public void setSlotMinutesAfter(int slotMinutesAfter)
    {
        this.slotMinutesAfter = slotMinutesAfter;
    }

    /**
     * @return {@link #participantCount}
     */
    public int getParticipantCount()
    {
        return participantCount;
    }

    /**
     * @param participantCount sets the {@link #participantCount}
     */
    public void setParticipantCount(int participantCount)
    {
        this.participantCount = participantCount;
    }

    /**
     * @return {@link #participantNotificationEnabled}
     */
    public boolean isParticipantNotificationEnabled()
    {
        return participantNotificationEnabled;
    }

    /**
     * @param participantNotificationEnabled sets the {@link #participantNotificationEnabled}
     */
    public void setParticipantNotificationEnabled(boolean participantNotificationEnabled)
    {
        this.participantNotificationEnabled = participantNotificationEnabled;
    }

    /**
     * @return {@link #serviceSpecifications}
     */
    public List<ExecutableServiceSpecification> getServiceSpecifications()
    {
        return serviceSpecifications;
    }

    /**
     * @param serviceSpecification to be added to the {@link #serviceSpecifications}
     */
    public void addServiceSpecification(ExecutableServiceSpecification serviceSpecification)
    {
        serviceSpecifications.add(serviceSpecification);
    }

    public static final String MEETING_NAME = "meetingName";
    public static final String MEETING_DESCRIPTION = "meetingDescription";
    public static final String SLOT_MINUTES_BEFORE = "slotMinutesBefore";
    public static final String SLOT_MINUTES_AFTER = "slotMinutesAfter";
    public static final String PARTICIPANT_COUNT = "participantCount";
    public static final String PARTICIPANT_NOTIFICATION_ENABLED = "participantNotificationEnabled";
    public static final String SERVICE_SPECIFICATIONS = "serviceSpecifications";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(MEETING_NAME, meetingName);
        dataMap.set(MEETING_DESCRIPTION, meetingDescription);
        dataMap.set(SLOT_MINUTES_BEFORE, slotMinutesBefore);
        dataMap.set(SLOT_MINUTES_AFTER, slotMinutesAfter);
        dataMap.set(PARTICIPANT_COUNT, participantCount);
        dataMap.set(PARTICIPANT_NOTIFICATION_ENABLED, participantNotificationEnabled);
        dataMap.set(SERVICE_SPECIFICATIONS, serviceSpecifications);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        meetingName = dataMap.getString(MEETING_NAME);
        meetingDescription = dataMap.getString(MEETING_DESCRIPTION);
        slotMinutesBefore = dataMap.getInt(SLOT_MINUTES_BEFORE, 0);
        slotMinutesAfter = dataMap.getInt(SLOT_MINUTES_AFTER, 0);
        participantCount = dataMap.getInt(PARTICIPANT_COUNT);
        participantNotificationEnabled = dataMap.getBool(PARTICIPANT_NOTIFICATION_ENABLED);
        serviceSpecifications = dataMap.getList(SERVICE_SPECIFICATIONS, ExecutableServiceSpecification.class);
    }
}
