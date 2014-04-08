package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import org.joda.time.Interval;

/**
 * Summary of {@link cz.cesnet.shongo.controller.api.Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationSummary extends IdentifiedComplexType
{
    /**
     * @see Type
     */
    private Type type;

    /**
     * Allocated date/time slot.
     */
    private Interval slot;

    /**
     * Allocated resource-id.
     */
    private String resourceId;

    /**
     * Allocated room license count.
     */
    private Integer roomLicenseCount;

    /**
     * Allocated room name.
     */
    private String roomName;

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
     * @return {@link #resourceId}
     */
    public String getResourceId()
    {
        return resourceId;
    }

    /**
     * @param resourceId sets the {@link #resourceId}
     */
    public void setResourceId(String resourceId)
    {
        this.resourceId = resourceId;
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

    private static final String TYPE = "type";
    private static final String SLOT = "slot";
    private static final String RESOURCE_ID = "resourceId";
    private static final String ROOM_LICENSE_COUNT = "roomLicenseCount";
    private static final String ROOM_NAME = "roomName";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(TYPE, type);
        dataMap.set(SLOT, slot);
        dataMap.set(RESOURCE_ID, resourceId);
        dataMap.set(ROOM_LICENSE_COUNT, roomLicenseCount);
        dataMap.set(ROOM_NAME, roomName);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        type = dataMap.getEnum(TYPE, Type.class);
        slot = dataMap.getInterval(SLOT);
        resourceId = dataMap.getString(RESOURCE_ID);
        roomLicenseCount = Integer.getInteger(ROOM_LICENSE_COUNT);
        roomName = dataMap.getString(ROOM_NAME);
    }

    /**
     * Type of {@link ReservationSummary}
     */
    public static enum Type
    {
        RESOURCE,
        ROOM,
        ALIAS,
        VALUE,
        RECORDING_SERVICE,
        OTHER
    }
}
