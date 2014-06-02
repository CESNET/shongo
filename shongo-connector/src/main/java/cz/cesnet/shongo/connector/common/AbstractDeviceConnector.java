package cz.cesnet.shongo.connector.common;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.api.ConnectorConfiguration;
import cz.cesnet.shongo.api.ConnectorStatus;
import cz.cesnet.shongo.connector.api.DeviceConfiguration;
import cz.cesnet.shongo.api.DeviceConnectorStatus;
import org.joda.time.Duration;

/**
 * {@link AbstractConnector} for managed device.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
abstract public class AbstractDeviceConnector extends AbstractConnector
{
    /**
     * Timeout for {@link #getConnectionState} in milliseconds.
     */
    public static final int CONNECTION_STATE_TIMEOUT = 500;

    /**
     * Timeout option
     */
    public static final String OPTION_TIMEOUT = "timeout";
    public static final Duration OPTION_TIMEOUT_DEFAULT = Duration.standardSeconds(30);

    /**
     * {@link DeviceAddress} of managed device.
     */
    protected DeviceAddress deviceAddress;

    /**
     * Name of managed device.
     */
    private String deviceName;

    /**
     * Description of managed device.
     */
    private String deviceDescription;

    /**
     * Serial number of managed device.
     */
    private String deviceSerialNumber;

    /**
     * Software version of managed device.
     */
    private String deviceSoftwareVersion;

    /**
     * Request timeout to device in milliseconds.
     */
    protected int requestTimeout;

    /**
     * @return {@link #deviceAddress}
     */
    public DeviceAddress getDeviceAddress()
    {
        return deviceAddress;
    }

    /**
     * @return {@link #deviceName}
     */
    public String getDeviceName()
    {
        return deviceName;
    }

    /**
     * @param deviceName sets the {@link #deviceName}
     */
    public void setDeviceName(String deviceName)
    {
        this.deviceName = deviceName;
    }

    /**
     * @return {@link #deviceDescription}
     */
    public String getDeviceDescription()
    {
        return deviceDescription;
    }

    /**
     * @param deviceDescription sets the {@link #deviceDescription}
     */
    public void setDeviceDescription(String deviceDescription)
    {
        this.deviceDescription = deviceDescription;
    }

    /**
     * @return {@link #deviceSerialNumber}
     */
    public String getDeviceSerialNumber()
    {
        return deviceSerialNumber;
    }

    /**
     * @param deviceSerialNumber sets the {@link #deviceSerialNumber}
     */
    public void setDeviceSerialNumber(String deviceSerialNumber)
    {
        this.deviceSerialNumber = deviceSerialNumber;
    }

    /**
     * @return {@link #deviceSoftwareVersion}
     */
    public String getDeviceSoftwareVersion()
    {
        return deviceSoftwareVersion;
    }

    /**
     * @param deviceSoftwareVersion sets the {@link #deviceSoftwareVersion}
     */
    public void setDeviceSoftwareVersion(String deviceSoftwareVersion)
    {
        this.deviceSoftwareVersion = deviceSoftwareVersion;
    }

    /**
     * @return {@link #requestTimeout}
     */
    public int getRequestTimeout()
    {
        return requestTimeout;
    }

    @Override
    public final void connect(ConnectorConfiguration configuration) throws CommandException
    {
        DeviceConfiguration deviceConfiguration = configuration.getDeviceConfiguration();
        this.configuration = configuration;
        this.deviceAddress = deviceConfiguration.getAddress();
        this.requestTimeout = (int) configuration.getOptionDuration(OPTION_TIMEOUT, OPTION_TIMEOUT_DEFAULT).getMillis();

        String userName = deviceConfiguration.getUserName();
        String password = deviceConfiguration.getPassword();
        connect(deviceAddress, userName, password);
    }

    @Override
    public ConnectorStatus getStatus()
    {
        DeviceConnectorStatus deviceConnectorStatus = new DeviceConnectorStatus(super.getStatus());
        if (isConnected()) {
            deviceConnectorStatus.setState(ConnectorStatus.State.AVAILABLE);
        }
        else {
            deviceConnectorStatus.setState(ConnectorStatus.State.NOT_AVAILABLE);
        }
        deviceConnectorStatus.setDeviceAddress(deviceAddress);
        deviceConnectorStatus.setDeviceName(deviceName);
        deviceConnectorStatus.setDeviceDescription(deviceDescription);
        deviceConnectorStatus.setDeviceSerialNumber(deviceSerialNumber);
        deviceConnectorStatus.setDeviceSoftwareVersion(deviceSoftwareVersion);
        return deviceConnectorStatus;
    }

    /**
     * @return true whether connector is connected to manage device, false otherwise
     */
    public final boolean isConnected()
    {
        ConnectionState state = getConnectionState();
        return state.equals(ConnectionState.CONNECTED) || state.equals(ConnectionState.LOOSELY_CONNECTED);
    }

    /**
     * Connect to device.
     *
     * @param deviceAddress
     * @param username
     * @param password
     * @throws CommandException
     */
    public abstract void connect(DeviceAddress deviceAddress, String username, String password) throws CommandException;

    /**
     * @return true if this {@link AbstractDeviceConnector} is connected to device, false otherwise
     */
    public abstract ConnectionState getConnectionState();


    /**
     * State of connection between the connector and the managed device.
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

        /**
         * The connection has been lost and the connector is trying to reconnect.
         */
        RECONNECTING,
    }
}
