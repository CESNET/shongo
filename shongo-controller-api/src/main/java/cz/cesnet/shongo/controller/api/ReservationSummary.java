package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import org.joda.time.Interval;

import java.util.HashSet;
import java.util.Set;

/**
 * Summary of {@link cz.cesnet.shongo.controller.api.Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationSummary extends IdentifiedComplexType
{
    /**
     * User-id of an user who created the {@link AbstractReservationRequest}
     * based on which this {@link ReservationSummary} was allocated.
     */
    private String userId;

    /**
     * Id of an {@link AbstractReservationRequest} based on which this {@link ReservationSummary} was allocated.
     */
    private String reservationRequestId;

    /**
     * Id of an {@link AbstractReservationRequest} which is parent to this {@code reservationRequestId} if any.
     */
    private String parentReservationRequestId;

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
     * Allocated alias types.
     */
    private String aliasTypes;

    /**
     * Allocated value.
     */
    private String value;

    /**
     * Description of ReservationRequest
     */
    private String reservationRequestDescription;

    /**
     * If reservation is writable by user (who listed them)
     */
    private Boolean isWritableByUser;

    /**
     * @return {@link #userId}
     */
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    /**
     * @return {@link #reservationRequestId}
     */
    public String getReservationRequestId()
    {
        return reservationRequestId;
    }

    /**
     * @param reservationRequestId sets the {@link #reservationRequestId}
     */
    public void setReservationRequestId(String reservationRequestId)
    {
        this.reservationRequestId = reservationRequestId;
    }

    public String getReservationRequestDescription() {
        return reservationRequestDescription;
    }

    public void setReservationRequestDescription(String reservationRequestDescription) {
        this.reservationRequestDescription = reservationRequestDescription;
    }

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

    /**
     * @return {@link #aliasTypes}
     */
    public String getAliasTypes()
    {
        return aliasTypes;
    }

    /**
     * @return {@link #aliasTypes} as {@link AliasType}s
     */
    public Set<AliasType> getAliasTypesSet()
    {
        Set<AliasType> aliasTypes = new HashSet<AliasType>();
        if (this.aliasTypes != null) {
            for (String aliasType : this.aliasTypes.split(",")) {
                aliasTypes.add(AliasType.valueOf(aliasType));
            }
        }
        return aliasTypes;
    }

    /**
     * @param aliasTypes sets the {@link #aliasTypes}
     */
    public void setAliasTypes(String aliasTypes)
    {
        this.aliasTypes = aliasTypes;
    }

    /**
     * @return {@link #value}
     */
    public String getValue()
    {
        return value;
    }

    /**
     * @param value sets the {@link #value}
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * @return {@link #parentReservationRequestId}
     */
    public String getParentReservationRequestId()
    {
        return parentReservationRequestId;
    }

    /**
     * @param parentReservationRequestId sets the {@link #parentReservationRequestId}
     */
    public void setParentReservationRequestId(String parentReservationRequestId)
    {
        this.parentReservationRequestId = parentReservationRequestId;
    }

    /**
     * @return {@link #isWritableByUser}
     */
    public Boolean getIsWritableByUser()
    {
        return isWritableByUser;
    }

    /**
     * @param isWritableByUser sets the {@link #isWritableByUser}
     */
    public void setIsWritableByUser(Boolean isWritableByUser)
    {
        this.isWritableByUser = isWritableByUser;
    }

    private static final String USER_ID = "userId";
    private static final String RESERVATION_REQUEST_ID = "reservationRequestId";
    private static final String TYPE = "type";
    private static final String SLOT = "slot";
    private static final String RESOURCE_ID = "resourceId";
    private static final String ROOM_LICENSE_COUNT = "roomLicenseCount";
    private static final String ROOM_NAME = "roomName";
    private static final String ALIAS_TYPES = "aliasTypes";
    private static final String VALUE = "value";
    private static final String RESERVATION_REQUEST_DESCRIPTION = "reservationRequestDescription";
    private static final String IS_WRITABLE_BY_USER = "isWritableByUser";
    private static final String PARENT_RESERVATION_REQUEST_ID = "parentReservationRequestId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(USER_ID, userId);
        dataMap.set(RESERVATION_REQUEST_ID, reservationRequestId);
        dataMap.set(TYPE, type);
        dataMap.set(SLOT, slot);
        dataMap.set(RESOURCE_ID, resourceId);
        dataMap.set(ROOM_LICENSE_COUNT, roomLicenseCount);
        dataMap.set(ROOM_NAME, roomName);
        dataMap.set(ALIAS_TYPES, aliasTypes);
        dataMap.set(VALUE, value);
        dataMap.set(RESERVATION_REQUEST_DESCRIPTION,reservationRequestDescription);
        dataMap.set(PARENT_RESERVATION_REQUEST_ID, parentReservationRequestId);
        dataMap.set(IS_WRITABLE_BY_USER, isWritableByUser);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        userId = dataMap.getString(USER_ID);
        reservationRequestId = dataMap.getString(RESERVATION_REQUEST_ID);
        type = dataMap.getEnum(TYPE, Type.class);
        slot = dataMap.getInterval(SLOT);
        resourceId = dataMap.getString(RESOURCE_ID);
        roomLicenseCount = dataMap.getInteger(ROOM_LICENSE_COUNT);
        roomName = dataMap.getString(ROOM_NAME);
        aliasTypes = dataMap.getString(ALIAS_TYPES);
        value = dataMap.getString(VALUE);
        reservationRequestDescription = dataMap.getString(RESERVATION_REQUEST_DESCRIPTION);
        parentReservationRequestId = dataMap.getString(PARENT_RESERVATION_REQUEST_ID);
        isWritableByUser = dataMap.getBoolean(IS_WRITABLE_BY_USER);
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
