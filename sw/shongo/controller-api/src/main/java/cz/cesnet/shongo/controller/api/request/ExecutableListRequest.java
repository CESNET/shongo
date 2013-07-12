package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.Reservation;
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
    private boolean includeHistory;

    private Set<Class<? extends Executable>> executableClasses = new HashSet<Class<? extends Executable>>();

    public ExecutableListRequest()
    {
        super(Sort.class);
    }

    public ExecutableListRequest(SecurityToken securityToken)
    {
        super(Sort.class, securityToken);
    }

    public boolean isIncludeHistory()
    {
        return includeHistory;
    }

    public void setIncludeHistory(boolean includeHistory)
    {
        this.includeHistory = includeHistory;
    }

    public Set<Class<? extends Executable>> getExecutableClasses()
    {
        return Collections.unmodifiableSet(executableClasses);
    }

    public void setExecutableClasses(Set<Class<? extends Executable>> executableClasses)
    {
        this.executableClasses = executableClasses;
    }

    public void addExecutableClass(Class<? extends Executable> executableClass)
    {
        this.executableClasses.add(executableClass);
    }

    public static enum Sort
    {
        SLOT
    }

    private static final String INCLUDE_HISTORY = "includeHistory";
    private static final String EXECUTABLE_CLASSES = "executableClasses";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(INCLUDE_HISTORY, includeHistory);
        dataMap.set(EXECUTABLE_CLASSES, executableClasses);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        includeHistory = dataMap.getBool(INCLUDE_HISTORY);
        executableClasses = (Set) dataMap.getSet(EXECUTABLE_CLASSES, Class.class);
    }
}
