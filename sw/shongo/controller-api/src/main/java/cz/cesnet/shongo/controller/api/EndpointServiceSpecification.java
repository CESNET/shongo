package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

/**
 * Specification of a service for an endpoint.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class EndpointServiceSpecification extends Specification
{
    /**
     * @see Type
     */
    private Type type;

    /**
     * Identifier of endpoint {@link Executable}.
     */
    private String endpointId;

    /**
     * Specifies whether the service should be automatically enabled for the booked time slot.
     */
    private boolean enabled;

    /**
     * Constructor.
     */
    public EndpointServiceSpecification()
    {
    }

    /**
     * @return new {@link EndpointServiceSpecification} with {@link Type#RECORDING}
     */
    public static EndpointServiceSpecification createRecording()
    {
        EndpointServiceSpecification endpointServiceSpecification = new EndpointServiceSpecification();
        endpointServiceSpecification.setType(Type.RECORDING);
        endpointServiceSpecification.setEnabled(true);
        return endpointServiceSpecification;
    }

    /**
     * @param endpointId sets the {@link #endpointId}
     * @return new {@link EndpointServiceSpecification} with {@link Type#RECORDING}
     */
    public static EndpointServiceSpecification createRecording(String endpointId)
    {
        EndpointServiceSpecification endpointServiceSpecification = createRecording();
        endpointServiceSpecification.setEndpointId(endpointId);
        endpointServiceSpecification.setEnabled(true);
        return endpointServiceSpecification;
    }

    /**
     * @return new {@link EndpointServiceSpecification} with {@link Type#RECORDING}
     */
    public static EndpointServiceSpecification createStreaming()
    {
        EndpointServiceSpecification endpointServiceSpecification = new EndpointServiceSpecification();
        endpointServiceSpecification.setType(Type.STREAMING);
        return endpointServiceSpecification;
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
     * @return {@link #endpointId}
     */
    public String getEndpointId()
    {
        return endpointId;
    }

    /**
     * @param endpointId sets the {@link #endpointId}
     */
    public void setEndpointId(String endpointId)
    {
        this.endpointId = endpointId;
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
    private static final String ENDPOINT_ID = "endpointId";
    private static final String ENABLED = "enabled";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(TYPE, type);
        dataMap.set(ENDPOINT_ID, endpointId);
        dataMap.set(ENABLED, enabled);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        type = dataMap.getEnumRequired(TYPE, Type.class);
        endpointId = dataMap.getString(ENDPOINT_ID);
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
