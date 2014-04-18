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
     * @see Type
     */
    private Type type;

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
     * @return new {@link ExecutableServiceSpecification} with {@link Type#RECORDING}
     */
    public static ExecutableServiceSpecification createRecording()
    {
        ExecutableServiceSpecification executableServiceSpecification = new ExecutableServiceSpecification();
        executableServiceSpecification.setType(Type.RECORDING);
        executableServiceSpecification.setEnabled(true);
        return executableServiceSpecification;
    }

    /**
     * @param executableId sets the {@link #executableId}
     * @return new {@link ExecutableServiceSpecification} with {@link Type#RECORDING}
     */
    public static ExecutableServiceSpecification createRecording(String executableId)
    {
        ExecutableServiceSpecification executableServiceSpecification = createRecording();
        executableServiceSpecification.setExecutableId(executableId);
        executableServiceSpecification.setEnabled(true);
        return executableServiceSpecification;
    }

    /**
     * @return new {@link ExecutableServiceSpecification} with {@link Type#RECORDING}
     */
    public static ExecutableServiceSpecification createStreaming()
    {
        ExecutableServiceSpecification executableServiceSpecification = new ExecutableServiceSpecification();
        executableServiceSpecification.setType(Type.STREAMING);
        return executableServiceSpecification;
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

    private static final String TYPE = "type";
    private static final String EXECUTABLE_ID = "executableId";
    private static final String ENABLED = "enabled";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(TYPE, type);
        dataMap.set(EXECUTABLE_ID, executableId);
        dataMap.set(ENABLED, enabled);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        type = dataMap.getEnumRequired(TYPE, Type.class);
        executableId = dataMap.getString(EXECUTABLE_ID);
        enabled = dataMap.getBool(ENABLED);
    }

    /**
     * Type of service.
     */
    public static enum Type
    {
        /**
         * Recording service.
         */
        RECORDING,

        /**
         * Streaming service.
         */
        STREAMING
    }
}
