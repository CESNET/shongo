package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.util.DeviceAddress;

/**
 * {@link ConnectorStatus} for devices.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DeviceConnectorStatus extends ConnectorStatus
{
    private DeviceAddress deviceAddress;
    private String deviceName;
    private String deviceDescription;
    private String deviceSerialNumber;
    private String deviceSoftwareVersion;

    public DeviceConnectorStatus()
    {
    }

    public DeviceConnectorStatus(ConnectorStatus connectorStatus)
    {
        setName(connectorStatus.getName());
        setState(connectorStatus.getState());
    }

    public DeviceAddress getDeviceAddress()
    {
        return deviceAddress;
    }

    public void setDeviceAddress(DeviceAddress deviceAddress)
    {
        this.deviceAddress = deviceAddress;
    }

    public String getDeviceName()
    {
        return deviceName;
    }

    public void setDeviceName(String deviceName)
    {
        this.deviceName = deviceName;
    }

    public String getDeviceDescription()
    {
        return deviceDescription;
    }

    public void setDeviceDescription(String deviceDescription)
    {
        this.deviceDescription = deviceDescription;
    }

    public String getDeviceSerialNumber()
    {
        return deviceSerialNumber;
    }

    public void setDeviceSerialNumber(String deviceSerialNumber)
    {
        this.deviceSerialNumber = deviceSerialNumber;
    }

    public String getDeviceSoftwareVersion()
    {
        return deviceSoftwareVersion;
    }

    public void setDeviceSoftwareVersion(String deviceSoftwareVersion)
    {
        this.deviceSoftwareVersion = deviceSoftwareVersion;
    }

    @Override
    public String toString()
    {
        return String.format("DeviceConnectorStatus (name: %s, state: %s, address: %s)",
                getName(), getState(), getDeviceAddress());
    }
}
