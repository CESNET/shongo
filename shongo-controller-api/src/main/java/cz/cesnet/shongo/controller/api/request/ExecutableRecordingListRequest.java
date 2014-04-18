package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.Recording;
import cz.cesnet.shongo.controller.api.SecurityToken;

/**
 * {@link ListRequest} for {@link Recording}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutableRecordingListRequest extends SortableListRequest<ExecutableRecordingListRequest.Sort>
{
    private String executableId;

    public ExecutableRecordingListRequest()
    {
        super(Sort.class);
    }

    public ExecutableRecordingListRequest(SecurityToken securityToken)
    {
        super(Sort.class, securityToken);
    }

    public String getExecutableId()
    {
        return executableId;
    }

    public void setExecutableId(String executableId)
    {
        this.executableId = executableId;
    }

    public static enum Sort
    {
        NAME,
        START,
        DURATION
    }

    private static final String EXECUTABLE_ID = "executableId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(EXECUTABLE_ID, executableId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        executableId = dataMap.getString(EXECUTABLE_ID);
    }
}
