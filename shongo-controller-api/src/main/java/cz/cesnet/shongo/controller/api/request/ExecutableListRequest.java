package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.AbstractRoomExecutable;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link ListRequest} for {@link Executable}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutableListRequest extends SortableListRequest<ExecutableListRequest.Sort>
{
    /**
     * Specifies whether {@link Executable}s which aren't allocated by any reservation should also be returned.
     */
    private boolean history;

    /**
     * Specifies which {@link ExecutableSummary.Type}s can be returned. Empty means all possible.
     */
    private Set<ExecutableSummary.Type> types = new HashSet<ExecutableSummary.Type>();

    /**
     * Resource of the executable.
     */
    private String resourceId;

    /**
     * Specifies identifier for {@link AbstractRoomExecutable} which must be used by returned {@link Executable}s
     * (e.g., {@link cz.cesnet.shongo.controller.api.UsedRoomExecutable}).
     */
    private String roomId;

    /**
     * Room license count.
     */
    private String roomLicenseCount;

    /**
     * Specifies user-id for user which must participates in returned {@link Executable}s.
     */
    private String participantUserId;

    /**
     * Constructor.
     */
    public ExecutableListRequest()
    {
        super(Sort.class);
    }

    /**
     * Constructor.
     *
     * @param securityToken sets the {@link #securityToken}
     */
    public ExecutableListRequest(SecurityToken securityToken)
    {
        super(Sort.class, securityToken);
    }

    /**
     * @return {@link #history}
     */
    public boolean isHistory()
    {
        return history;
    }

    /**
     * @param history sets the {@link #history}
     */
    public void setHistory(boolean history)
    {
        this.history = history;
    }

    /**
     * @return {@link #types}
     */
    public Set<ExecutableSummary.Type> getTypes()
    {
        return Collections.unmodifiableSet(types);
    }

    /**
     * @param types sets the {@link #types}
     */
    public void setTypes(Set<ExecutableSummary.Type> types)
    {
        this.types = types;
    }

    /**
     * @param type to be added to the {@link #types}
     */
    public void addType(ExecutableSummary.Type type)
    {
        this.types.add(type);
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
     * @return {@link #roomLicenseCount}
     */
    public String getRoomLicenseCount()
    {
        return roomLicenseCount;
    }

    /**
     * @param roomLicenseCount sets the {@link #roomLicenseCount}
     */
    public void setRoomLicenseCount(String roomLicenseCount)
    {
        this.roomLicenseCount = roomLicenseCount;
    }

    /**
     * @return {@link #participantUserId}
     */
    public String getParticipantUserId()
    {
        return participantUserId;
    }

    /**
     * @param participantUserId sets the {@link #participantUserId}
     */
    public void setParticipantUserId(String participantUserId)
    {
        this.participantUserId = participantUserId;
    }

    /**
     * Available sort options.
     */
    public static enum Sort
    {
        ROOM_NAME,
        ROOM_LICENSE_COUNT,
        ROOM_TECHNOLOGY,
        SLOT,
        STATE
    }

    private static final String HISTORY = "history";
    private static final String TYPES = "types";
    private static final String RESOURCE_ID = "resourceId";
    private static final String ROOM_ID = "roomId";
    private static final String ROOM_LICENSE_COUNT = "roomLicenseCount";
    private static final String PARTICIPANT_USER_ID = "participantUserId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(HISTORY, history);
        dataMap.set(TYPES, types);
        dataMap.set(RESOURCE_ID, resourceId);
        dataMap.set(ROOM_ID, roomId);
        dataMap.set(ROOM_LICENSE_COUNT, roomLicenseCount);
        dataMap.set(PARTICIPANT_USER_ID, participantUserId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        history = dataMap.getBool(HISTORY);
        types = (Set) dataMap.getSet(TYPES, ExecutableSummary.Type.class);
        resourceId = dataMap.getString(RESOURCE_ID);
        roomId = dataMap.getString(ROOM_ID);
        roomLicenseCount = dataMap.getString(ROOM_LICENSE_COUNT);
        participantUserId = dataMap.getString(PARTICIPANT_USER_ID);
    }
}
