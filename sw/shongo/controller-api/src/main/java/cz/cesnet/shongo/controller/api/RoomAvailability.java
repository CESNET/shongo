package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.DataMap;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents that room should be made available to participants for joining.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomAvailability extends AbstractComplexType
{
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
     * Specifies whether configured participants should  be notified about the room.
     */
    private boolean participantNotificationEnabled = false;

    /**
     * Specifies message by which the participants should be notified.
     */
    private String participantNotification;

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
     * @return {@link #participantNotification}
     */
    public String getParticipantNotification()
    {
        return participantNotification;
    }

    /**
     * @param participantNotification sets the {@link #participantNotification}
     */
    public void setParticipantNotification(String participantNotification)
    {
        this.participantNotification = participantNotification;
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

    public static final String SLOT_MINUTES_BEFORE = "slotMinutesBefore";
    public static final String SLOT_MINUTES_AFTER = "slotMinutesAfter";
    public static final String PARTICIPANT_COUNT = "participantCount";
    public static final String PARTICIPANT_NOTIFICATION_ENABLED = "participantNotificationEnabled";
    public static final String PARTICIPANT_NOTIFICATION = "participantNotification";
    public static final String SERVICE_SPECIFICATIONS = "serviceSpecifications";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(SLOT_MINUTES_BEFORE, slotMinutesBefore);
        dataMap.set(SLOT_MINUTES_AFTER, slotMinutesAfter);
        dataMap.set(PARTICIPANT_COUNT, participantCount);
        dataMap.set(PARTICIPANT_NOTIFICATION_ENABLED, participantNotificationEnabled);
        dataMap.set(PARTICIPANT_NOTIFICATION, participantNotification);
        dataMap.set(SERVICE_SPECIFICATIONS, serviceSpecifications);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        slotMinutesBefore = dataMap.getInt(SLOT_MINUTES_BEFORE, 0);
        slotMinutesAfter = dataMap.getInt(SLOT_MINUTES_AFTER, 0);
        participantCount = dataMap.getInt(PARTICIPANT_COUNT);
        participantNotificationEnabled = dataMap.getBool(PARTICIPANT_NOTIFICATION_ENABLED);
        participantNotification = dataMap.getString(PARTICIPANT_NOTIFICATION);
        serviceSpecifications = dataMap.getList(SERVICE_SPECIFICATIONS, ExecutableServiceSpecification.class);
    }
}
