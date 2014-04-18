package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import org.joda.time.Interval;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents summary of an allocated {@link Executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutableSummary extends IdentifiedComplexType
{
    /**
     * @see Type
     */
    private Type type;

    /**
     * Slot of the {@link ExecutableSummary}.
     */
    private Interval slot;

    /**
     * Current state of the {@link ExecutableSummary}.
     */
    private ExecutableState state;

    /**
     * Room name.
     */
    private String roomName;

    /**
     * Room technologies.
     */
    private Set<Technology> roomTechnologies = new HashSet<Technology>();

    /**
     * License count.
     */
    private Integer roomLicenseCount;

    /**
     * Room description.
     */
    private String roomDescription;

    /**
     * Used room id for {@link Type#USED_ROOM}.
     */
    private String roomId;

    /**
     * Slot of the earliest room usage.
     */
    private Interval roomUsageSlot;

    /**
     * State of the earliest room usage.
     */
    private ExecutableState roomUsageState;

    /**
     * License count of the earliest room usage.
     */
    private Integer roomUsageLicenseCount;

    /**
     * Number of existing room usages.
     */
    private int roomUsageCount = 0;

    /**
     * @return {@link #type}
     */
    public Type getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(Type type)
    {
        this.type = type;
    }

    /**
     * @return {@link #slot}
     */
    public Interval getSlot()
    {
        return slot;
    }

    /**
     * @param slot sets the {@link #slot}
     */
    public void setSlot(Interval slot)
    {
        this.slot = slot;
    }

    /**
     * @return {@link #state}
     */
    public ExecutableState getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(ExecutableState state)
    {
        this.state = state;
    }

    /**
     * @return {@link #roomName}
     */
    public String getRoomName()
    {
        return roomName;
    }

    /**
     * @param roomName sets the {@link #roomName}
     */
    public void setRoomName(String roomName)
    {
        this.roomName = roomName;
    }

    /**
     * @return {@link #roomTechnologies}
     */
    public Set<Technology> getRoomTechnologies()
    {
        return roomTechnologies;
    }

    /**
     * @param roomTechnologies sets the {@link #roomTechnologies}
     */
    public void setRoomTechnologies(Set<Technology> roomTechnologies)
    {
        this.roomTechnologies = roomTechnologies;
    }

    /**
     * @param technology to be added to the {@link #roomTechnologies}
     */
    public void addTechnology(Technology technology)
    {
        roomTechnologies.add(technology);
    }

    /**
     * @return {@link #roomLicenseCount}
     */
    public Integer getRoomLicenseCount()
    {
        return roomLicenseCount;
    }

    /**
     * @param roomLicenseCount sets the {@link #roomLicenseCount}
     */
    public void setRoomLicenseCount(Integer roomLicenseCount)
    {
        this.roomLicenseCount = roomLicenseCount;
    }

    /**
     * @return {@link #roomDescription}
     */
    public String getRoomDescription()
    {
        return roomDescription;
    }

    /**
     * @param roomDescription sets the {@link #roomDescription}
     */
    public void setRoomDescription(String roomDescription)
    {
        this.roomDescription = roomDescription;
    }

    /**
     * @return {@link #roomId}
     */
    public String getRoomId()
    {
        return roomId;
    }

    /**
     * @param roomId sets the {@link #roomId}
     */
    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }

    /**
     * @return {@link #roomUsageSlot}
     */
    public Interval getRoomUsageSlot()
    {
        return roomUsageSlot;
    }

    /**
     * @param roomUsageSlot sets the {@link #roomUsageSlot}
     */
    public void setRoomUsageSlot(Interval roomUsageSlot)
    {
        this.roomUsageSlot = roomUsageSlot;
    }

    /**
     * @return {@link #roomUsageState}
     */
    public ExecutableState getRoomUsageState()
    {
        return roomUsageState;
    }

    /**
     * @param roomUsageState sets the {@link #roomUsageState}
     */
    public void setRoomUsageState(ExecutableState roomUsageState)
    {
        this.roomUsageState = roomUsageState;
    }

    /**
     * @return {@link #roomUsageLicenseCount}
     */
    public Integer getRoomUsageLicenseCount()
    {
        return roomUsageLicenseCount;
    }

    /**
     * @param roomUsageLicenseCount sets the {@link #roomUsageLicenseCount}
     */
    public void setRoomUsageLicenseCount(Integer roomUsageLicenseCount)
    {
        this.roomUsageLicenseCount = roomUsageLicenseCount;
    }

    /**
     * @return {@link #roomUsageCount}
     */
    public int getRoomUsageCount()
    {
        return roomUsageCount;
    }

    /**
     * @param roomUsageCount sets the {@link #roomUsageCount}
     */
    public void setRoomUsageCount(int roomUsageCount)
    {
        this.roomUsageCount = roomUsageCount;
    }

    private static final String TYPE = "type";
    private static final String SLOT = "slot";
    private static final String STATE = "state";
    private static final String ROOM_NAME = "roomName";
    private static final String ROOM_TECHNOLOGIES = "roomTechnologies";
    private static final String ROOM_LICENSE_COUNT = "roomLicenseCount";
    private static final String ROOM_DESCRIPTION = "roomDescription";
    private static final String ROOM_ID = "roomId";
    private static final String ROOM_USAGE_SLOT = "roomUsageSlot";
    private static final String ROOM_USAGE_STATE = "roomUsageState";
    private static final String ROOM_USAGE_LICENSE_COUNT = "roomUsageLicenseCount";
    private static final String ROOM_USAGE_COUNT = "roomUsageCount";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(TYPE, type);
        dataMap.set(SLOT, slot);
        dataMap.set(STATE, state);
        dataMap.set(ROOM_NAME, roomName);
        dataMap.set(ROOM_TECHNOLOGIES, roomTechnologies);
        dataMap.set(ROOM_LICENSE_COUNT, roomLicenseCount);
        dataMap.set(ROOM_DESCRIPTION, roomDescription);
        dataMap.set(ROOM_ID, roomId);
        dataMap.set(ROOM_USAGE_SLOT, roomUsageSlot);
        dataMap.set(ROOM_USAGE_STATE, roomUsageState);
        dataMap.set(ROOM_USAGE_LICENSE_COUNT, roomUsageLicenseCount);
        dataMap.set(ROOM_USAGE_COUNT, roomUsageCount);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        type = dataMap.getEnum(TYPE, Type.class);
        slot = dataMap.getInterval(SLOT);
        state = dataMap.getEnum(STATE, ExecutableState.class);
        roomName = dataMap.getString(ROOM_NAME);
        roomTechnologies = dataMap.getSet(ROOM_TECHNOLOGIES, Technology.class);
        roomLicenseCount = dataMap.getInteger(ROOM_LICENSE_COUNT);
        roomDescription = dataMap.getString(ROOM_DESCRIPTION);
        roomId = dataMap.getString(ROOM_ID);
        roomUsageSlot = dataMap.getInterval(ROOM_USAGE_SLOT);
        roomUsageState = dataMap.getEnum(ROOM_USAGE_STATE, ExecutableState.class);
        roomUsageLicenseCount = dataMap.getInteger(ROOM_USAGE_LICENSE_COUNT);
        roomUsageCount = dataMap.getInt(ROOM_USAGE_COUNT);
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + "{id=" + id
                + ", type=" + type
                + ", technology=" + roomTechnologies
                + "}";
    }

    /**
     * Type of {@link ExecutableSummary}.
     */
    public static enum Type
    {
        ROOM,
        USED_ROOM,
        OTHER
    }
}
