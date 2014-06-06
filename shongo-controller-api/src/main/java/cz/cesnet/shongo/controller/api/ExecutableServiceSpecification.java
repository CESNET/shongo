package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

/**
 * Specification of a service for an {@link Executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutableServiceSpecification extends Specification
{
    /**
     * Identifier of required {@link Resource}.
     */
    private String resourceId;

    /**
     * Identifier of {@link Executable}.
     */
    private String executableId;

    /**
     * Specifies whether the service should be automatically enabled for the booked time slot.
     */
    private boolean enabled;

    /**
     * Constructor.
     */
    public ExecutableServiceSpecification()
    {
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
     * @return {@link #executableId}
     */
    public String getExecutableId()
    {
        return executableId;
    }

    /**
     * @param executableId sets the {@link #executableId}
     */
    public void setExecutableId(String executableId)
    {
        this.executableId = executableId;
    }

    /**
     * @return {@link #enabled}
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * @param enabled sets the {@link #enabled}
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    private static final String RESOURCE_ID = "resourceId";
    private static final String EXECUTABLE_ID = "executableId";
    private static final String ENABLED = "enabled";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESOURCE_ID, resourceId);
        dataMap.set(EXECUTABLE_ID, executableId);
        dataMap.set(ENABLED, enabled);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        resourceId = dataMap.getString(RESOURCE_ID);
        executableId = dataMap.getString(EXECUTABLE_ID);
        enabled = dataMap.getBool(ENABLED);
    }
}
