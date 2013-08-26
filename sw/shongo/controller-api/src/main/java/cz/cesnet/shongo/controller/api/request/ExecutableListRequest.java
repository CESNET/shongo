package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link ListRequest} for {@link cz.cesnet.shongo.controller.api.Executable}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutableListRequest extends SortableListRequest<ExecutableListRequest.Sort>
{
    private boolean history;

    private Set<ExecutableSummary.Type> types = new HashSet<ExecutableSummary.Type>();

    private String roomId;



    public ExecutableListRequest()
    {
        super(Sort.class);
    }

    public ExecutableListRequest(SecurityToken securityToken)
    {
        super(Sort.class, securityToken);
    }

    public boolean isHistory()
    {
        return history;
    }

    public void setHistory(boolean history)
    {
        this.history = history;
    }

    public Set<ExecutableSummary.Type> getTypes()
    {
        return Collections.unmodifiableSet(types);
    }

    public void setTypes(Set<ExecutableSummary.Type> types)
    {
        this.types = types;
    }

    public void addType(ExecutableSummary.Type type)
    {
        this.types.add(type);
    }

    public String getRoomId()
    {
        return roomId;
    }

    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }

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
    private static final String ROOM_ID = "roomId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(HISTORY, history);
        dataMap.set(TYPES, types);
        dataMap.set(ROOM_ID, roomId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        history = dataMap.getBool(HISTORY);
        types = (Set) dataMap.getSet(TYPES, ExecutableSummary.Type.class);
        roomId = dataMap.getString(ROOM_ID);
    }
}
