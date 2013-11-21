package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.ExecutableService;
import cz.cesnet.shongo.controller.api.SecurityToken;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link ListRequest} for {@link ExecutableService}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutableServiceListRequest extends ListRequest
{
    private String executableId;

    private Set<Class<? extends ExecutableService>> serviceClasses = new HashSet<Class<? extends ExecutableService>>();

    public ExecutableServiceListRequest()
    {
    }

    public ExecutableServiceListRequest(SecurityToken securityToken)
    {
        super(securityToken);
    }

    public ExecutableServiceListRequest(SecurityToken securityToken, String executableId)
    {
        super(securityToken);
        this.executableId = executableId;
    }

    public ExecutableServiceListRequest(SecurityToken securityToken, String executableId, Class<? extends ExecutableService> serviceClass)
    {
        super(securityToken);
        this.executableId = executableId;
        this.serviceClasses.add(serviceClass);
    }

    public String getExecutableId()
    {
        return executableId;
    }

    public void setExecutableId(String executableId)
    {
        this.executableId = executableId;
    }

    public Set<Class<? extends ExecutableService>> getServiceClasses()
    {
        return serviceClasses;
    }

    public void setServiceClasses(Set<Class<? extends ExecutableService>> serviceClasses)
    {
        this.serviceClasses = serviceClasses;
    }

    public void addServiceClass(Class<? extends ExecutableService> serviceClass)
    {
        serviceClasses.add(serviceClass);
    }

    private static final String EXECUTABLE_ID = "executableId";
    private static final String SERVICE_CLASSES = "serviceClasses";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(EXECUTABLE_ID, executableId);
        dataMap.set(SERVICE_CLASSES, serviceClasses);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        executableId = dataMap.getString(EXECUTABLE_ID);
        serviceClasses = (Set) dataMap.getSet(SERVICE_CLASSES, Class.class);
    }
}
