package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.util.Address;

/**
 * Information about a connector.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ConnectorInfo
{
    /**
     * State of connection between the connector and the device it manages.
     */
    public static enum ConnectionState
    {
        /**
         * The connection is established.
         */
        CONNECTED,
        /**
         * The connection was established but is not maintained (the communication is stateless).
         */
        LOOSELY_CONNECTED,
        /**
         * The connection is not established.
         */
        DISCONNECTED,
    }

    private String name;
    private DeviceInfo deviceInfo;
    private Address deviceAddress;
    private ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private DeviceState deviceState;


    public ConnectorInfo(String name)
    {
        this.name = name;
    }

    /**
     * @return connection state to the device
     */
    public ConnectionState getConnectionState()
    {
        return connectionState;
    }

    /**
     * @param connectionState connection state to the device
     */
    public void setConnectionState(ConnectionState connectionState)
    {
        this.connectionState = connectionState;
    }

    /**
     * @return static info about the device managed by this connector
     */
    public DeviceInfo getDeviceInfo()
    {
        return deviceInfo;
    }

    /**
     * @param deviceInfo the device managed by this connector (must be a resource of type ManagedDevice)
     */
    public void setDeviceInfo(DeviceInfo deviceInfo)
    {
        this.deviceInfo = deviceInfo;
    }

    public Address getDeviceAddress()
    {
        return deviceAddress;
    }

    public void setDeviceAddress(Address deviceAddress)
    {
        this.deviceAddress = deviceAddress;
    }

    /**
     * @return state of the device, maintained by the connector for performance reasons
     */
    public DeviceState getDeviceState()
    {
        return deviceState;
    }

    /**
     * @param deviceState state of the device, maintained by the connector for performance reasons
     */
    public void setDeviceState(DeviceState deviceState)
    {
        this.deviceState = deviceState;
    }

    /**
     * @return the connector name
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name the connector name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        String s = name + "; " + connectionState;
        if (deviceInfo != null) {
            s += "; (device: " + deviceInfo + ")";
        }
        return s;
    }
}
